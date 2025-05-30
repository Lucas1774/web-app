import chroma from "chroma-js";
import { useEffect, useMemo, useRef, useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import "../../Table.css";
import { get, post } from "../../api";
import commerceIcon from "../../assets/images/commerce.png";
import * as constants from "../../constants";
import useDebounce from "../../hooks/useDebounce";
import LoginForm from "../LoginForm";
import Spinner from "../Spinner";
import { handleError } from "../errorHandler";
import "./Portfolio.css";
import RecommendationPopup from "./RecommendationPopup";
import TransactionPopup from "./TransactionPopup";

const Portfolio = () => {

    const [tableData, setTableData] = useState(null);
    const [displayData, setDisplayData] = useState([]);
    const [selectedIds, setSelectedIds] = useState(new Set());
    const [filterValue, setFilterValue] = useState({});
    const [message, setMessage] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isLoginFormVisible, setIsLoginFormVisible] = useState(false);
    const [filters, setFilters] = useState({});
    const [order, setOrder] = useState({ key: null, order: constants.DESC });
    const [isShowRealTime, setIsShowRealTime] = useState(false);
    const [isShowAllData, setIsShowAllData] = useState(false);
    const [popupContent, setPopupContent] = useState(null);
    const [transactionPopupContent, setTransactionPopupContent] = useState(null);
    const [count, setCount] = useState("");

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
                    return row[key]?.toString().toLowerCase().includes(filters[key].toLowerCase());
                } else if (constants.PORTFOLIO_META.DATATYPE[key] === constants.DATE) {
                    return filters[key] === "" || row[key]?.toLocaleDateString() === new Date(filters[key]).toLocaleDateString();
                } else if (key === "select") {
                    return !filters[key] || selectedIds.has(row[constants.ID_KEY]);
                } else {
                    return true;
                }
            });
        });

        const ordered = [...filtered].sort((a, b) => {
            if (order.key === null) {
                return 0;
            };
            if (order.order === constants.DESC) {
                if (a[order.key] == null) return 1;
                if (b[order.key] == null) return -1;
                return a[order.key] < b[order.key] ? 1 : -1;
            } else {
                if (a[order.key] == null) return 1;
                if (b[order.key] == null) return -1;
                return a[order.key] > b[order.key] ? 1 : -1;
            }
        });

        setDisplayData(ordered);
    }, [tableData, filters, order, selectedIds]);

    const getData = async (dynamically = false, all = false) => {
        setIsLoading(true);
        try {
            const url = all ? "/portfolio/stand/all" : `/portfolio/stand?dynamic=${dynamically}`;
            const resp = await get(url);
            const data = resp.data.map((item) => {
                const newest = item.recommendation.reduce(
                    (best, curr) =>
                        new Date(curr.date) > new Date(best.date) ? curr : best,
                    item.recommendation[0]
                );
                return {
                    [constants.ID_KEY]: item.symbol.id,
                    [constants.SYMBOL_NAME_KEY]: item.symbol.name,
                    [constants.LAST_MOVE_DATE_KEY]: item.lastMoveDate ? new Date(item.lastMoveDate) : null,
                    [constants.RECOMMENDATION_DATE_KEY]: newest ? new Date(newest.date) : null,
                    [constants.RECOMMENDATION_ACTION_KEY]: newest?.action,
                    [constants.PRICE_KEY]: item.price,
                    [constants.OPEN_KEY]: item.open,
                    [constants.HIGH_KEY]: item.high,
                    [constants.LOW_KEY]: item.low,
                    [constants.VOLUME_KEY]: item.volume,
                    [constants.QUANTITY_KEY]: item.quantity,
                    [constants.AVERAGE_COST_KEY]: item.averageCost,
                    [constants.POSITION_VALUE_KEY]: item.positionValue,
                    [constants.PNL_KEY]: item.pnL,
                    [constants.PERCENT_PNL_KEY]: item.percentPnl,
                    [constants.NET_RELATIVE_POSITION_KEY]: item.netRelativePosition,
                    [constants.RECOMMENDATION_CONFIDENCE_KEY]: newest?.confidence,
                    [constants.RECOMMENDATION_RATIONALE_KEY]: newest?.rationale,
                };
            });

            setTableData(data);
            setDisplayData(data);
            setIsShowRealTime(dynamically);
            setIsShowAllData(all);
        } catch (error) {
            if (error.response?.status === 401) {
                setMessage("Unauthorized");
                setTimeout(() => {
                    setMessage(null);
                }, constants.TIMEOUT_DELAY);
            } else {
                handleError("Error fetching data", error);
            }
        } finally {
            setIsLoading(false);
        }
    };

    const getRecommendations = async (sendFixmeRequest, overwrite, count = undefined) => {
        setIsLoading(true);
        try {
            let path;
            if (count !== undefined) {
                path = `/recommendations/random/${count}`
            } else {
                const ids = visibleSelectedIds().join(',')
                if (ids.length === 0) {
                    setMessage("Select at least one row");
                    setTimeout(() => {
                        setMessage(null);
                    }, constants.TIMEOUT_DELAY);
                    return;
                }
                path = `/recommendations/${ids}`;
            }
            const resp = await get(`${path}?sendFixmeRequest=${sendFixmeRequest}&overwrite=${overwrite}`);
            const data = new Map(
                resp.data.map(item => [
                    item.symbol.id,
                    {
                        [constants.SYMBOL_NAME_KEY]: item.symbol.name,
                        [constants.RECOMMENDATION_ACTION_KEY]: item.action,
                        [constants.RECOMMENDATION_CONFIDENCE_KEY]: item.confidence,
                        [constants.RECOMMENDATION_DATE_KEY]: new Date(item.date),
                        [constants.RECOMMENDATION_RATIONALE_KEY]: item.rationale,
                    }
                ])
            );

            const updated = tableData.map(row => {
                const id = row[constants.ID_KEY];
                if (data.has(id)) {
                    return { ...row, ...data.get(id) };
                }
                return row;
            });

            setTableData(updated);
            setDisplayData(updated);
            setIsShowAllData(false);
        } catch (error) {
            if (error.response?.status === 401) {
                setMessage("Unauthorized");
                setTimeout(() => {
                    setMessage(null);
                }, constants.TIMEOUT_DELAY);
            } else {
                handleError("Error fetching data", error);
            }
        } finally {
            setIsLoading(false);
        }
    }

    const updateNewsSentiment = async () => {
        setIsLoading(true);
        try {
            const resp = await get(`/sentiment/historic?from=${new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString().slice(0, 10)}`);
            if (resp.data.length === 0) {
                setMessage("No news to update");
                setTimeout(() => {
                    setMessage(null);
                }, constants.TIMEOUT_DELAY);
                return;
            }
            setMessage("Updated sentiment for " + resp.data.length + " news items");
            setTimeout(() => {
                setMessage(null);
            }, constants.TIMEOUT_DELAY);
            return;
        } catch (error) {
            if (error.response?.status === 401) {
                setMessage("Unauthorized");
                setTimeout(() => {
                    setMessage(null);
                }, constants.TIMEOUT_DELAY);
            } else {
                handleError("Error fetching data", error);
            }
        } finally {
            setIsLoading(false);
        }
    }

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

    const allIds = () => displayData.map(row => row[constants.ID_KEY]);
    const isAllSelected = () => allIds().every(id => selectedIds.has(id));

    const visibleSelectedIds = () => {
        const visibleIdSet = new Set(
            displayData.map(row => row[constants.ID_KEY])
        );
        return Array.from(selectedIds).filter(id =>
            visibleIdSet.has(id)
        );
    };

    const scales = useMemo(() => {
        const extractValidNumbers = (key) => displayData.map(d => d[key]).filter(v => v != null && !Number.isNaN(v)).map(Number);

        const percentPnls = extractValidNumbers(constants.PERCENT_PNL_KEY);
        const confidences = extractValidNumbers(constants.RECOMMENDATION_CONFIDENCE_KEY);
        const netPositions = extractValidNumbers(constants.NET_RELATIVE_POSITION_KEY);

        return {
            [constants.PERCENT_PNL_KEY]: chroma.scale(['red', 'yellow', 'green']).domain([Math.min(...percentPnls), 0, Math.max(...percentPnls)]),
            [constants.RECOMMENDATION_CONFIDENCE_KEY]: chroma.scale(['red', 'yellow', 'green']).domain([
                Math.min(...confidences),
                (Math.min(...confidences) + Math.max(...confidences)) / 2,
                Math.max(...confidences)
            ]),
            [constants.NET_RELATIVE_POSITION_KEY]: chroma.scale(['red', 'yellow', 'green']).domain([Math.min(...netPositions), 0, Math.max(...netPositions)]),
        };
    }, [displayData]);

    const renderCell = (key, row) => {
        const value = row[key];
        const type = constants.PORTFOLIO_META.DATATYPE[key];

        if (key === constants.RECOMMENDATION_ACTION_KEY) {
            return <td key={key} onClick={(e) => {
                e.preventDefault();
                setPopupContent(row[constants.RECOMMENDATION_RATIONALE_KEY]);
            }}>{value}</td>;
        }
        if (key === constants.PERCENT_PNL_KEY || key === constants.RECOMMENDATION_CONFIDENCE_KEY || key === constants.NET_RELATIVE_POSITION_KEY) {
            return <td key={key} style={{
                backgroundColor: value != null ? scales[key](value).hex() : "black",
                color: "black"
            }}>{value != null ? value.toFixed(2) : null}%</td>;
        }
        if (type === constants.NUMBER || type === constants.STRING) {
            return <td key={key}>{value}</td>;
        }
        if (type === constants.DATE) {
            return <td key={key}>{value?.toLocaleDateString()}</td>;
        }
        if (key === constants.EDIT_KEY) {
            return (
                <td key={key} title={"buy / sell"} style={{ padding: '5px' }}>
                    <Button className="icon-button" onClick={(e) => {
                        e.preventDefault();
                        setTransactionPopupContent({
                            [constants.ID_KEY]: row[constants.ID_KEY],
                            [constants.SYMBOL_NAME_KEY]: row[constants.SYMBOL_NAME_KEY],
                            onPopupClose: () => setTransactionPopupContent(null)
                        });
                    }}>
                        <img src={commerceIcon} alt=""></img>
                    </Button>
                </td>
            );
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
    if (popupContent != null) {
        return <div className="app custom-table portfolio">
            <RecommendationPopup content={popupContent} onPopupClose={() => setPopupContent(null)} />
        </div>;
    }
    if (transactionPopupContent != null) {
        return <div className="app custom-table portfolio">
            <TransactionPopup id={transactionPopupContent[constants.ID_KEY]}
                name={transactionPopupContent[constants.SYMBOL_NAME_KEY]}
                onPopupClose={() => { setTransactionPopupContent(null); getData(false, isShowAllData) }} />
        </div>
    }

    return (
        <>
            <h1 id="portfolio">Portfolio</h1>
            <div className="app custom-table portfolio">
                {tableData &&
                    <>
                        <Form onSubmit={(e) => {
                            e.preventDefault();
                            getRecommendations(e.target[3].checked, e.target[4].checked);
                        }}>
                            <Button className={isShowAllData ? "fifty-percent" : "thirty-percent"} type="submit" variant="success">Recommend</Button>
                            <Button className="thirty-percent" style={isShowAllData ? { position: "absolute", visibility: "hidden" } : {}} onClick={() => { getData(!isShowRealTime); }}>{
                                isShowRealTime ? "Last close" : "Real time"
                            }</Button>
                            <Button className={isShowAllData ? "fifty-percent" : "thirty-percent"} onClick={() => {
                                Object.values(inputsRef.current).forEach((input) => {
                                    if (input) {
                                        if (!input.hasOwnProperty("checked")) {
                                            input.value = ""
                                        } else {
                                            input.checked = false;
                                        }
                                    }
                                });
                                setFilters({});

                            }}>Clear filters</Button>
                            <div className="flex-div">
                                <Form.Check type="checkbox" label="Pre-request" />
                                <Form.Check type="checkbox" label="Overwrite" />
                            </div>
                        </Form>
                        <Form onSubmit={(e) => {
                            e.preventDefault();
                            const amount = e.target[5].value;
                            if (!amount) {
                                setMessage("Specify an amount");
                                setTimeout(() => {
                                    setMessage(null);
                                }, constants.TIMEOUT_DELAY);
                                return;
                            }
                            getRecommendations(e.target[3].checked, e.target[4].checked, amount)
                        }}>
                            <Button className={"thirty-percent"} type="submit" variant="success">Random recommendations</Button>
                            <Button className="thirty-percent" onClick={updateNewsSentiment}>Update news sentiment</Button>
                            <Button className="thirty-percent" onClick={() => { isShowAllData ? getData() : getData(undefined, true); }}>{
                                isShowAllData ? "Show active data" : "Show all data"
                            }</Button>
                            <div className="flex-div">
                                <Form.Check type="checkbox" label="Pre-request" />
                                <Form.Check type="checkbox" label="Overwrite" />
                                <Form.Control type="number" placeholder="Amount" min="1" value={count} onChange={(e) => setCount(Number(e.target.value))} />
                            </div>
                        </Form>
                        <Table striped bordered hover responsive>
                            <thead>
                                <tr>
                                    <th>
                                        <Form.Check
                                            type="checkbox"
                                            checked={isAllSelected()}
                                            onChange={() => setSelectedIds(() => isAllSelected() ? new Set() : new Set(allIds()))}
                                        /></th>
                                    {constants.PORTFOLIO_META.KEYS.filter((key) => constants.PORTFOLIO_META.VISIBLE[key]).map((key) => (
                                        <th key={key}>{constants.PORTFOLIO_META.DISPLAY_NAME[key]}</th>
                                    ))}
                                </tr>
                                <tr>
                                    <th key="select">
                                        <Form.Check ref={(e) => inputsRef.current["select"] = e}
                                            type="checkbox"
                                            onChange={(e) => setFilterValue({
                                                column: "select",
                                                value: e.target.checked
                                            })}
                                        />
                                    </th>
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
                                            <td>
                                                <Form.Check
                                                    type="checkbox"
                                                    checked={selectedIds.has(id)}
                                                    onChange={() => {
                                                        setSelectedIds(prev => {
                                                            const next = new Set(prev);
                                                            if (next.has(id)) {
                                                                next.delete(id);
                                                            } else {
                                                                next.add(id);
                                                            }
                                                            return next;
                                                        });
                                                    }} /></td>
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
