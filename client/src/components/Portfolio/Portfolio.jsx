import chroma from "chroma-js";
import PropTypes from "prop-types";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import MultiSelectDdl from "../../MultiSelectDdl";
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

const Portfolio = ({ onClose = () => { } }) => {

    const [tableData, setTableData] = useState(null);
    const [displayData, setDisplayData] = useState([]);
    const [models, setModels] = useState([]);
    const [selectedModels, setSelectedModels] = useState([]);
    const [selectedRandomModels, setSelectedRandomModels] = useState([]);
    const [selectedIds, setSelectedIds] = useState(new Set());
    const [filterValue, setFilterValue] = useState({});
    const [message, setMessage] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isLoginFormVisible, setIsLoginFormVisible] = useState(false);
    const [filters, setFilters] = useState({});
    const [order, setOrder] = useState({ key: null, order: constants.DESC });
    const [isShowAllData, setIsShowAllData] = useState(false);
    const [popupContent, setPopupContent] = useState(null);
    const [transactionPopupContent, setTransactionPopupContent] = useState(null);
    const [count, setCount] = useState("");

    const inputsRef = useRef({});
    const overwriteRef = useRef(null);
    const afterHoursContextRef = useRef(null);
    const overwriteRandomRef = useRef(null);
    const afterHoursContextRandomRef = useRef(null);
    const amountRef = useRef(null);
    const useOldNewsRef = useRef(null);
    const useOldNewsRandomRef = useRef(null);
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
        if (!tableData) return;

        const filtered = tableData.filter((row) => {
            return Object.keys(filters).every((key) => {
                if (key === constants.RECOMMENDATION_CONFIDENCE_KEY) {
                    return isNaN(filters[key]) || row[key] >= filters[key];
                } else if (key === constants.RECOMMENDATION_MODEL_KEY) {
                    return filters[key] === "" || row[key]?.toString().toLowerCase() === (filters[key].toLowerCase())
                } else if (constants.PORTFOLIO_META.DATATYPE[key] === constants.NUMBER) {
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

    const getModelName = (model) => {
        if (!model) {
            return "";
        }
        if (model === "gpt-4.1" || model.includes("gpt-4.1-")) {
            return "gpt-4.1";
        }
        if (model === "grok-3" || model.includes("grok-3-")) {
            return "grok-3";
        }
        return model;
    }

    const getData = useCallback(async (all = false) => {
        const url = all ? "/portfolio/stand/all" : "/portfolio/stand";
        setIsLoading(true);
        try {
            const resp = await get(url);
            const data = resp.data.map((item) => {
                const recommendation = item.recommendation;
                return {
                    [constants.ID_KEY]: item.symbol.id,
                    [constants.REAL_TIME_KEY]: "N",
                    [constants.SYMBOL_NAME_KEY]: item.symbol.name,
                    [constants.SECTOR_KEY]: item.symbol.sector,
                    [constants.LAST_MOVE_DATE_KEY]: item.lastMoveDate ? new Date(item.lastMoveDate) : null,
                    [constants.RECOMMENDATION_DATE_KEY]: recommendation ? new Date(recommendation.date) : null,
                    [constants.RECOMMENDATION_ACTION_KEY]: recommendation?.action,
                    [constants.RECOMMENDATION_CONFIDENCE_KEY]: recommendation?.confidence,
                    [constants.RECOMMENDATION_MODEL_KEY]: getModelName(recommendation?.model),
                    [constants.RECOMMENDATION_NEWS_KEY]: recommendation?.news?.map(n => ({
                        id: n.id,
                        date: n.date,
                        headline: n.headline,
                        summary: n.summary,
                        sentiment: n.sentiment,
                        sentimentConfidence: n.sentimentConfidence
                    })) ?? [],
                    [constants.PRICE_KEY]: item.price,
                    [constants.OPEN_KEY]: item.open,
                    [constants.HIGH_KEY]: item.high,
                    [constants.LOW_KEY]: item.low,
                    [constants.PERCENT_DAY_CHANGE_KEY]: item.percentDayChange,
                    [constants.VOLUME_KEY]: item.volume,
                    [constants.QUANTITY_KEY]: item.quantity,
                    [constants.AVERAGE_COST_KEY]: item.averageCost,
                    [constants.POSITION_VALUE_KEY]: item.positionValue,
                    [constants.PNL_KEY]: item.pnL,
                    [constants.PERCENT_PNL_KEY]: item.percentPnl,
                    [constants.NET_RELATIVE_POSITION_KEY]: item.netRelativePosition,
                    [constants.RECOMMENDATION_RATIONALE_KEY]: recommendation?.rationale,
                };
            });

            setTableData(data);
            setDisplayData(data);
            setIsShowAllData(all);
        } catch (error) {
            handleError("Error fetching data", error);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        const init = async () => {
            setIsLoading(true);
            try {
                await get("/authentication/check-auth");
                await getData();
                const res = await get("/recommendations/models");
                setModels(res.data);
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

        init();
    }, [getData]);

    const getDataByRow = async (dynamic) => {
        const ids = visibleSelectedIds().join(',')
        if (ids.length === 0) {
            setMessage("Select at least one row");
            setTimeout(() => {
                setMessage(null);
            }, constants.TIMEOUT_DELAY);
            return;
        }
        setIsLoading(true);
        try {
            const resp = await get(`/portfolio/stand/${ids}?dynamic=${dynamic}`);
            const data = new Map(
                resp.data.map(item => [
                    item.symbol.id,
                    {
                        [constants.REAL_TIME_KEY]: dynamic ? "Y" : "N",
                        [constants.PRICE_KEY]: item.price,
                        [constants.OPEN_KEY]: item.open,
                        [constants.HIGH_KEY]: item.high,
                        [constants.LOW_KEY]: item.low,
                        [constants.PERCENT_DAY_CHANGE_KEY]: item.percentDayChange,
                        [constants.VOLUME_KEY]: item.volume,
                        [constants.PNL_KEY]: item.pnL,
                        [constants.PERCENT_PNL_KEY]: item.percentPnl,
                        [constants.NET_RELATIVE_POSITION_KEY]: item.netRelativePosition,
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

    const getRecommendations = async (overwrite, afterHoursContext, useOldNews, selectedModels, count = undefined) => {
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
        let modelsParam = selectedModels.length > 0 ? `&models=${selectedModels.join(',')}` : '';
        setIsLoading(true);
        try {
            const resp = await get(`${path}?overwrite=${overwrite}&afterHoursContext=${afterHoursContext}&useOldNews=${useOldNews}${modelsParam}`);
            const data = new Map(
                resp.data.map(item => [
                    item.symbol.id,
                    {
                        [constants.RECOMMENDATION_DATE_KEY]: new Date(item.date),
                        [constants.RECOMMENDATION_ACTION_KEY]: item.action,
                        [constants.RECOMMENDATION_CONFIDENCE_KEY]: item.confidence,
                        [constants.RECOMMENDATION_MODEL_KEY]: getModelName(item.model),
                        [constants.RECOMMENDATION_RATIONALE_KEY]: item.rationale,
                        [constants.RECOMMENDATION_NEWS_KEY]: item.news?.map(n => ({
                            id: n.id,
                            date: n.date,
                            headline: n.headline,
                            summary: n.summary,
                            sentiment: n.sentiment,
                            sentimentConfidence: n.sentimentConfidence
                        })) ?? []
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

    const updateNews = async () => {
        const ids = visibleSelectedIds().join(',')
        if (ids.length === 0) {
            setMessage("Select at least one row");
            setTimeout(() => {
                setMessage(null);
            }, constants.TIMEOUT_DELAY);
            return;
        }
        setIsLoading(true);
        try {
            const resp = await get(`/news/last/${ids}`);
            if (resp.data.length === 0) {
                setMessage("No new news");
                setTimeout(() => {
                    setMessage(null);
                }, constants.TIMEOUT_DELAY);
                return;
            }
            setMessage("Updated " + resp.data.length + " news");
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

        const percentDayChanges = extractValidNumbers(constants.PERCENT_DAY_CHANGE_KEY);
        const percentPnls = extractValidNumbers(constants.PERCENT_PNL_KEY);
        const confidences = extractValidNumbers(constants.RECOMMENDATION_CONFIDENCE_KEY);
        const netPositions = extractValidNumbers(constants.NET_RELATIVE_POSITION_KEY);

        return {
            [constants.PERCENT_DAY_CHANGE_KEY]: chroma.scale(['red', 'yellow', 'green']).domain([Math.min(...percentDayChanges), 0, Math.max(...percentDayChanges)]),
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
                setPopupContent({
                    rationale: row[constants.RECOMMENDATION_RATIONALE_KEY],
                    news: row[constants.RECOMMENDATION_NEWS_KEY]
                });
            }}>{value}</td>;
        }
        if (key === constants.PERCENT_DAY_CHANGE_KEY || key === constants.PERCENT_PNL_KEY
            || key === constants.RECOMMENDATION_CONFIDENCE_KEY || key === constants.NET_RELATIVE_POSITION_KEY) {
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
                onPopupClose={() => { setTransactionPopupContent(null); }}
                onTransactionSuccessful={() => { getData(false) }} />
        </div>
    }

    return (
        <div className="app custom-table portfolio">
            {!isLoading && !message && <div className="flex-div" style={{ height: "0" }}>
                <div></div>
                <Button className="app restart popup-icon" onClick={onClose}>X</Button>
            </div>}
            <h1 id="portfolio">Portfolio</h1>
            {tableData &&
                <>
                    <Form onSubmit={(e) => {
                        e.preventDefault();
                        getRecommendations(overwriteRef.current.checked, afterHoursContextRef.current.checked,
                            useOldNewsRef.current.checked, selectedModels);
                    }}>
                        <Button className="twenty-five-percent" type="submit" variant="success">Recommend</Button>
                        <Button className="twenty-five-percent" onClick={updateNews}>Fetch latest news</Button>
                        <Button className="twenty-five-percent" onClick={() => { getDataByRow(false); }}>Last close</Button>
                        <Button className="twenty-five-percent" onClick={() => { getDataByRow(true); }}>Real time</Button>
                        <div className="flex-div">
                            <Form.Check ref={overwriteRef} type="checkbox" label="Overwrite" />
                            <Form.Check ref={afterHoursContextRef} type="checkbox" label="After hours context" />
                            <Form.Check ref={useOldNewsRef} type="checkbox" label="Use old news" />
                            <MultiSelectDdl title={"Models"} options={models} selectedOptions={selectedModels} setSelectedOptions={setSelectedModels} />
                            <Form.Control style={{ visibility: "hidden" }} /> {/* Big hack to uniform style */}
                        </div>
                    </Form>
                    <Form onSubmit={(e) => {
                        e.preventDefault();
                        const amount = amountRef.current.value;
                        if (!amount) {
                            setMessage("Specify an amount");
                            setTimeout(() => {
                                setMessage(null);
                            }, constants.TIMEOUT_DELAY);
                            return;
                        }
                        getRecommendations(overwriteRandomRef.current.checked, afterHoursContextRandomRef.current.checked,
                            useOldNewsRandomRef.current.checked, selectedRandomModels, amount)
                    }}>
                        <Button className="thirty-percent" type="submit" variant="success">Random recommendations</Button>
                        <Button className="thirty-percent" onClick={() => { getData(!isShowAllData); }}>{
                            isShowAllData ? "Show active data" : "Show all data"
                        }</Button>
                        <Button className="thirty-percent" onClick={() => {
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
                            <Form.Check ref={overwriteRandomRef} type="checkbox" label="Overwrite" />
                            <Form.Check ref={afterHoursContextRandomRef} type="checkbox" label="After hours context" />
                            <Form.Check ref={useOldNewsRandomRef} type="checkbox" label="Use old news" />
                            <MultiSelectDdl title={"Models"} options={models} selectedOptions={selectedRandomModels} setSelectedOptions={setSelectedRandomModels} />
                            <Form.Control ref={amountRef} type="number" placeholder="Amount" min="1" value={count} onChange={(e) => setCount(Number(e.target.value))} />
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
                                                    value: constants.PORTFOLIO_META.DATATYPE[key] === constants.NUMBER ? Number(e.target.value) : e.target.value
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
    );
}

Portfolio.propTypes = {
    onClose: PropTypes.func,
};

export default Portfolio;
