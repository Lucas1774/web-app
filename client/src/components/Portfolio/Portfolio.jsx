import chroma from "chroma-js";
import { useEffect, useRef, useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import "../../Table.css";
import { get, post } from "../../api";
import * as constants from "../../constants";
import useDebounce from "../../hooks/useDebounce";
import LoginForm from "../LoginForm";
import Spinner from "../Spinner";
import { handleError } from "../errorHandler";
import "./Portfolio.css";

const Portfolio = () => {

    const [tableData, setTableData] = useState(null);
    const [displayData, setDisplayData] = useState(null);
    const [filterValue, setFilterValue] = useState({});
    const [message, setMessage] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isLoginFormVisible, setIsLoginFormVisible] = useState(false);
    const [filters, setFilters] = useState({});
    const [order, setOrder] = useState({ key: null, order: constants.DESC })
    const [percentPnlRange, setPercentPnlRange] = useState({ min: 0, max: 0 });
    const [netPositionRange, setNetPositionRange] = useState({ min: 0, max: 0 });
    const [confidenceRange, setConfidenceRange] = useState({ min: 0, max: 0 });

    const inputsRef = useRef({});
    const filterDebouncedValue = useDebounce(filterValue, constants.DEBOUNCE_DELAY);

    useEffect(() => {
        if (filterDebouncedValue?.value != null && filterDebouncedValue?.column != null) {
            setFilters(prevFilters => {
                const newFilters = { ...prevFilters };
                newFilters[filterDebouncedValue.column] = filterDebouncedValue.value;
                return newFilters;
            });
        }
    }, [filterDebouncedValue]);

    useEffect(() => {
        const checkAuth = async () => {
            setIsLoading(true);
            try {
                await get("/authentication/check-auth");
                await getData();
            } catch (error) {
                if (error.response?.status === 403) {
                    setIsLoginFormVisible(true);
                } else {
                    handleError("Error checking authentication", error);
                }
            } finally {
                setIsLoading(false);
            }
        };

        checkAuth();
    }, []);

    useEffect(() => {
        if (!tableData) return;

        const filtered = tableData.filter((row) => {
            return Object.keys(filters).every((key) => {
                if (constants.PORTFOLIO_META.DATATYPE[key] === constants.NUMBER) {
                    return isNaN(filters[key]) || row[key] === filters[key];
                } else if (constants.PORTFOLIO_META.DATATYPE[key] === constants.STRING) {
                    return row[key].toString().toLowerCase().includes(filters[key].toLowerCase());
                } else if (constants.PORTFOLIO_META.DATATYPE[key] === constants.DATE) {
                    return filters[key] === "" || row[key].toLocaleDateString() === new Date(filters[key]).toLocaleDateString();
                } else {
                    return true;
                }
            });
        });

        const percentPnls = filtered.map(d => d[constants.PERCENT_PNL_KEY]);
        const netPositions = filtered.map(d => d[constants.NET_RELATIVE_POSITION_KEY]);
        const confidences = filtered.map(d => d[constants.RECOMMENDATION_CONFIDENCE_KEY]);

        setPercentPnlRange({
            min: Math.min(...percentPnls),
            max: Math.max(...percentPnls),
        });
        setNetPositionRange({
            min: Math.min(...netPositions),
            max: Math.max(...netPositions),
        });
        setConfidenceRange({
            min: Math.min(...confidences),
            max: Math.max(...confidences),
        });

        const ordered = [...filtered].sort((a, b) => {
            if (order.key === null) {
                return 0;
            };
            if (order.order === constants.DESC) {
                return a[order.key] < b[order.key] ? 1 : -1;
            } else {
                return a[order.key] > b[order.key] ? 1 : -1;
            }
        });

        setDisplayData(ordered);
    }, [tableData, filters, order]);

    const getData = async () => {
        setIsLoading(true);
        try {
            const resp = await get("/portfolio/stand?mock=true&dynamic=false");
            const data = resp.data.map((item) => {
                const newest = item.recommendation.reduce(
                    (best, curr) =>
                        new Date(curr.date) > new Date(best.date) ? curr : best,
                    item.recommendation[0]
                );
                return {
                    [constants.ID_KEY]: item.symbol.id,
                    [constants.SYMBOL_NAME_KEY]: item.symbol.name,
                    [constants.LAST_MOVE_DATE_KEY]: new Date(item.lastMoveDate),
                    [constants.QUANTITY_KEY]: item.quantity,
                    [constants.AVERAGE_COST_KEY]: item.averageCost,
                    [constants.POSITION_VALUE_KEY]: item.positionValue,
                    [constants.PNL_KEY]: item.pnL,
                    [constants.PERCENT_PNL_KEY]: item.percentPnl,
                    [constants.NET_RELATIVE_POSITION_KEY]: item.netRelativePosition,
                    [constants.RECOMMENDATION_ACTION_KEY]: newest.action,
                    [constants.RECOMMENDATION_CONFIDENCE_KEY]: newest.confidence,
                    [constants.RECOMMENDATION_DATE_KEY]: new Date(newest.date),
                    [constants.RECOMMENDATION_RATIONALE_KEY]: newest.rationale,
                };
            });

            setTableData(data);
            setDisplayData(data);
        } catch (error) {
            handleError("Error fetching data", error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleLoginSubmit = async (event) => {
        event.preventDefault();
        const username = event.target[0].value.trim();
        const password = event.target[1].value.trim();
        const action = event.nativeEvent.submitter.value;
        if (!password || "validate" !== action) {
            setMessage("No password provided. Continuing as guest");
            setTimeout(() => {
                setMessage(null);
                setIsLoading(true);
                setIsLoginFormVisible(false);
                getData();
            }, constants.TIMEOUT_DELAY);
        } else {
            setIsLoading(true);
            try {
                await post('/authentication/login', { [constants.USERNAME]: username, [constants.PASSWORD]: password });
                setMessage("Login successful");
                setTimeout(() => {
                    setMessage(null);
                    setIsLoading(true);
                    setIsLoginFormVisible(false);
                    getData();
                }, constants.TIMEOUT_DELAY);
            } catch (error) {
                if (error.response?.status === 403) {
                    setMessage("Wrong credentials. Continuing as guest");
                    setTimeout(() => {
                        setMessage(null);
                        setIsLoading(true);
                        setIsLoginFormVisible(false);
                        getData();
                    }, constants.TIMEOUT_DELAY);
                } else {
                    handleError("Error sending data", error);
                }
            } finally {
                setIsLoading(false);
            }
        }
    };

    const handleOrderClick = (key) => {
        setOrder((prevOrder) => ({
            key,
            order: prevOrder.key === key
                ? (prevOrder.order === constants.DESC ? constants.ASC : constants.DESC)
                : constants.ASC
        }));
    };

    const renderCell = (key, row) => {
        const value = row[key];
        const type = constants.PORTFOLIO_META.DATATYPE[key];

        if (key === constants.PERCENT_PNL_KEY || key === constants.RECOMMENDATION_CONFIDENCE_KEY || key === constants.NET_RELATIVE_POSITION_KEY) {
            const domain = key === constants.PERCENT_PNL_KEY
                ? percentPnlRange : key === constants.RECOMMENDATION_CONFIDENCE_KEY
                    ? confidenceRange : netPositionRange;
            const scale = chroma.scale(['red', 'yellow', 'green']).domain([domain.min, 0, domain.max]);
            return <td key={key} style={{ backgroundColor: scale(value).hex(), color: "black" }}>{value.toFixed(2)}%</td>;
        }
        if (type === constants.NUMBER || type === constants.STRING) {
            return <td key={key}>{value}</td>;
        }
        if (type === constants.DATE) {
            return <td key={key}>{value.toLocaleDateString()}</td>;
        }
        return <td key={key}>{String(value)}</td>;
    };

    if (message) {
        return <div className="app custom-table portfolio"><div>{message}</div></div>;
    }
    if (isLoginFormVisible) {
        return <div className="app custom-table portfolio"><LoginForm onSubmit={handleLoginSubmit} /></div>;
    }
    if (isLoading || !tableData) {
        return <Spinner />;
    }

    return (
        <>
            <h1 id="portfolio">Portfolio</h1>
            <div className="app custom-table portfolio">
                {tableData &&
                    <>
                        <Button onClick={() => {
                            Object.values(inputsRef.current).forEach((input) => {
                                if (input) {
                                    input.value = "";
                                }
                            });
                            setFilters({});
                        }}>Clear filters</Button><Table striped bordered hover responsive>
                            <thead>
                                <tr>
                                    {constants.PORTFOLIO_META.KEYS.filter((key) => constants.PORTFOLIO_META.VISIBLE[key]).map((key) => (
                                        <th key={key}>{constants.PORTFOLIO_META.DISPLAY_NAME[key]}</th>
                                    ))}
                                </tr>
                                <tr>
                                    {constants.PORTFOLIO_META.KEYS.filter((key) => constants.PORTFOLIO_META.VISIBLE[key]).map((key) => (
                                        <th key={key}>
                                            {constants.PORTFOLIO_META.FILTERABLE[key] && (
                                                <Form.Control ref={(e) => inputsRef.current[key] = e}
                                                    type={constants.PORTFOLIO_META.DATATYPE[key] === constants.DATE ? "date" : "text"}
                                                    inputMode={constants.PORTFOLIO_META.DATATYPE[key] === constants.NUMBER ? "numeric" : "text"}
                                                    placeholder={constants.PORTFOLIO_META.DISPLAY_NAME[key]}
                                                    defaultValue={constants.PORTFOLIO_META.DATATYPE[key] === constants.NUMBER && isNaN(filters[key]) ? "" : filters[key]}
                                                    onChange={(e) => setFilterValue({
                                                        column: key,
                                                        value: constants.META.DATATYPE[key] === constants.NUMBER ? parseInt(e.target.value) : e.target.value
                                                    })}
                                                    onClick={(e) => e.target.select()} />
                                            )}
                                            {constants.PORTFOLIO_META.SORTABLE[key] && (
                                                <Button onClick={() => { handleOrderClick(key); }}>
                                                    {order.key === key
                                                        ? order.order === constants.ASC ? '▲' : '▼' : 'Sort'}
                                                </Button>
                                            )}
                                        </th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {displayData.map((row) => {
                                    const id = row[constants.ID_KEY];
                                    return (
                                        <tr key={id}>
                                            {constants.PORTFOLIO_META.KEYS
                                                .filter((key) => constants.PORTFOLIO_META.VISIBLE[key])
                                                .map((key) => renderCell(key, row))}
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </Table>
                    </>
                }
            </div>
        </>
    );
}

export default Portfolio;
