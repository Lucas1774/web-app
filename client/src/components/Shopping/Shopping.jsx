import { useCallback, useEffect, useRef, useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import { get, post } from "../../api";
import deleteIcon from "../../assets/images/bin.png";
import editIcon from "../../assets/images/edit.png";
import resetIcon from "../../assets/images/remove.png";
import * as constants from "../../constants";
import useDebounce from "../../hooks/useDebounce";
import DebounceableInput from "../DebounceableInput";
import { handleError } from "../errorHandler";
import LoginForm from "../LoginForm";
import Spinner from "../Spinner";
import ConfirmProductRemovalPopup from "./ConfirmProductRemovalPopup";
import EditProductPopup from "./EditProductPopup";
import EditSortablesPopup from "./EditSortablesPopup";
import "../../Table.css";

const Shopping = () => {
    const [tableData, setTableData] = useState(null);
    const [sortables, setSortables] = useState([]);
    const [sortablesType, setSortablesType] = useState(null);
    const [popup, setPopup] = useState(null);
    const [filterValue, setFilterValue] = useState({});
    const [message, setMessage] = useState(null);
    const [selectedProductData, setSelectedProductData] = useState({})
    const [isLoading, setIsLoading] = useState(true);
    const [isLoginFormVisible, setIsLoginFormVisible] = useState(false);
    const [isPopupVisible, setIsPopupVisible] = useState(false);
    const [filters, setFilters] = useState({});
    const [order, setOrder] = useState({ key: null, order: constants.DESC })
    const [isShowOnlyPositive, setIsShowOnlyPositive] = useState(false);
    const [isShowOnlyCommon, setIsShowOnlyCommon] = useState(false);

    const inputsRef = useRef({});
    const filterDebouncedValue = useDebounce(filterValue, constants.DEBOUNCE_DELAY)

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

    const getData = async () => {
        setIsLoading(true);
        try {
            const response = await get("/shopping/shopping");
            setTableData(response.data.map(item => ({
                [constants.ID_KEY]: item[constants.PRODUCT_KEY][constants.ID_KEY],
                [constants.NAME_KEY]: item[constants.PRODUCT_KEY][constants.NAME_KEY],
                [constants.QUANTITY_KEY]: item[constants.QUANTITY_KEY],
                [constants.IS_RARE_KEY]: item[constants.PRODUCT_KEY][constants.IS_RARE_KEY],
                [constants.ORDER_KEY]: item[constants.PRODUCT_KEY][constants.ORDER_KEY],
                [constants.CATEGORY_ID_KEY]: item[constants.PRODUCT_KEY][constants.CATEGORY_KEY]?.[constants.ID_KEY] ?? null,
                [constants.CATEGORY_KEY]: item[constants.PRODUCT_KEY][constants.CATEGORY_KEY]?.[constants.NAME_KEY] ?? "",
                [constants.CATEGORY_ORDER_KEY]: item[constants.PRODUCT_KEY][constants.CATEGORY_KEY]?.[constants.ORDER_KEY] ?? null,
            })));
        } catch (error) {
            handleError("Error fetching data", error);
        } finally {
            setIsLoading(false);
        }
    };

    const updateProductQuantity = useCallback(async (value, id) => {
        if (isNaN(value) || parseInt(value) < 0) {
            return;
        }
        try {
            await post('/shopping/update-product-quantity', {
                [constants.PRODUCT_KEY]: {
                    [constants.ID_KEY]: id,
                    [constants.TYPE_KEY]: "products"
                },
                [constants.QUANTITY_KEY]: parseInt(value),
            });
            setTableData(previous => previous.map(product =>
                product[constants.ID_KEY] === id
                    ? { ...product, [constants.QUANTITY_KEY]: value }
                    : product
            ));
        } catch (error) {
            handleError("Error sending data", error);
        }
    }, []);

    const updateProduct = async (id, name, isRare, categoryId, category) => {
        setIsLoading(true);
        try {
            await post('/shopping/update-product', {
                [constants.ID_KEY]: id,
                [constants.NAME_KEY]: name,
                [constants.IS_RARE_KEY]: isRare,
                [constants.TYPE_KEY]: "products",
                [constants.CATEGORY_KEY]: {
                    [constants.ID_KEY]: categoryId,
                    [constants.NAME_KEY]: category,
                    [constants.TYPE_KEY]: "categories"
                }
            });
            setMessage("Product " + name + " updated successfully");
            setTimeout(() => {
                setMessage(null);
                setIsLoading(true);
                setIsPopupVisible(false);
                getData();
            }, constants.TIMEOUT_DELAY);
        } catch (error) {
            if (error.response?.status === 401) {
                setMessage("Unauthorized");
                setTimeout(() => {
                    setMessage(null);
                }, constants.TIMEOUT_DELAY);
            } else {
                handleError("Error sending data", error);
            }
        } finally {
            setIsLoading(false);
        }
    };

    const removeProduct = async (id, name) => {
        setIsLoading(true);
        try {
            await post('/shopping/remove-product', { [constants.ID_KEY]: id, [constants.TYPE_KEY]: "products" });
            setMessage("Product " + name + " removed successfully");
            setTimeout(() => {
                setMessage(null);
                setIsLoading(true);
                getData();
            }, constants.TIMEOUT_DELAY);
        } catch (error) {
            handleError("Error sending data", error);
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

    const handleResetAll = async () => {
        setIsLoading(true);
        try {
            await post('/shopping/update-all-product-quantity', 0);
            setMessage("All quantities were set to 0");
            setTimeout(() => {
                setMessage(null);
                setIsLoading(true);
                getData();
            }, constants.TIMEOUT_DELAY);
        } catch (error) {
            handleError("Error sending data", error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleAddProductSubmit = async (event) => {
        event.preventDefault();
        const name = event.target[0].value.trim();
        if (!name) {
            return;
        }
        setIsLoading(true);
        try {
            await post('/shopping/new-product', name);
            setMessage("Product " + name + " added successfully");
            setTimeout(() => {
                setMessage(null);
                setIsLoading(true);
                getData();
            }, constants.TIMEOUT_DELAY);
        } catch (error) {
            if (error.response?.status === 409) {
                setMessage("Product already exists");
                setTimeout(() => {
                    setMessage(null);
                }, constants.TIMEOUT_DELAY);
            } else {
                handleError("Error sending data", error);
            }
        } finally {
            setIsLoading(false);
        }
    };

    const getPossibleCategories = async () => {
        setIsLoading(true);
        try {
            const response = await get("/shopping/get-possible-categories");
            setSortables(response.data);
            setSortablesType("categories");
        } catch (error) {
            handleError("Error fetching data", error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleEditProduct = (id, name, isRare, category) => {
        getPossibleCategories();
        setSelectedProductData({
            [constants.ID_KEY]: id,
            [constants.NAME_KEY]: name,
            [constants.IS_RARE_KEY]: isRare,
            [constants.CATEGORY_KEY]: category
        });
        setPopup("editProduct");
        setIsPopupVisible(true);
    };

    const showSortPopup = () => {
        setPopup("editSortables");
        setIsPopupVisible(true);
    };

    const handleRemoveProduct = async (id, name) => {
        setSelectedProductData({ [constants.ID_KEY]: id, [constants.NAME_KEY]: name });
        setPopup("removeProduct");
        setIsPopupVisible(true);
    };

    const handleOrderSave = async () => {
        const updatedSortables = sortables.map((sortable, index) => ({
            ...sortable,
            [constants.ORDER_KEY]: index + 1,
            [constants.TYPE_KEY]: sortablesType,
        }));
        // use sortablestype to enrich "sortables"
        setIsLoading(true);
        try {
            await post('/shopping/update-sortables', updatedSortables);
            setMessage("Elements successfully sorted");
            setTimeout(() => {
                setMessage(null);
                setIsLoading(true);
                setIsPopupVisible(false);
                getData();
            }, constants.TIMEOUT_DELAY);
        } catch (error) {
            if (error.response?.status === 401) {
                setMessage("Unauthorized");
                setTimeout(() => {
                    setMessage(null);
                }, constants.TIMEOUT_DELAY);
            } else {
                handleError("Error sending data", error);
            }
        } finally {
            setIsLoading(false);
        }
    };

    const handleOrderClick = (key) => {
        const actualKey = key === constants.CATEGORY_KEY ? constants.CATEGORY_ORDER_KEY : key === constants.NAME_KEY ? constants.ORDER_KEY : key;
        setOrder((prevOrder) => ({
            key: actualKey,
            order: prevOrder.key === actualKey
                ? (prevOrder.order === constants.DESC ? constants.ASC : constants.DESC)
                : constants.ASC
        }));
    };

    const applyFilters = (data, filters) => {
        return data.filter((row) => {
            return (!isShowOnlyPositive || row[constants.QUANTITY_KEY] !== 0)
                && (!isShowOnlyCommon || !row[constants.IS_RARE_KEY]) && Object.keys(filters).every((key) => {
                    if (constants.META.DATATYPE[key] === constants.NUMBER) {
                        return isNaN(filters[key]) || row[key] === filters[key];
                    } else if (constants.META.DATATYPE[key] === constants.STRING) {
                        return row[key].toString().toLowerCase().includes(filters[key].toLowerCase());
                    } else {
                        return true;
                    }
                });
        });
    };

    const applyOrder = (data, order) => {
        if (order.key === null) {
            return data;
        }
        return data.sort((a, b) => {
            if (order.order === constants.DESC) {
                return a[order.key] < b[order.key] ? 1 : -1;
            } else {
                return a[order.key] > b[order.key] ? 1 : -1;
            }
        });
    };

    const handleDebouncedChange = useCallback(
        (value, id) => updateProductQuantity(value, id),
        [updateProductQuantity]
    );

    const renderCell = (key, id, name, isRare, categoryId, category, quantity) => {
        if (key === constants.NAME_KEY) {
            return <td key={key} title={name} style={{ maxWidth: '100px' }}>{name}</td>;
        }
        if (key === constants.CATEGORY_KEY) {
            return <td key={key} title={category} style={{ maxWidth: '100px' }}>{category}</td>;
        }
        if (key === constants.QUANTITY_KEY) {
            return (
                <td key={key}>
                    <div className="cell-item-container">
                        <DebounceableInput
                            value={quantity}
                            id={id}
                            name={name}
                            inputMode="numeric"
                            onDebouncedChange={handleDebouncedChange}
                        />
                        <Button className="icon-button" onClick={() => {
                            if (quantity !== 0) {
                                updateProductQuantity(0, id, name)
                            }
                        }}>
                            <img src={resetIcon} alt=""></img>
                        </Button>
                    </div>
                </td>
            );
        }
        if (key === constants.EDIT_KEY) {
            return (
                <td key={key} title={constants.EDIT_KEY.toLowerCase()} style={{ padding: '5px' }}>
                    <Button className="icon-button" onClick={() => handleEditProduct(id, name, isRare, { [constants.CATEGORY_ID_KEY]: categoryId, [constants.CATEGORY_KEY]: category })}>
                        <img src={editIcon} alt=""></img>
                    </Button>
                </td>
            );
        }
        if (key === constants.REMOVE_KEY) {
            return (
                <td key={key} title={constants.REMOVE_KEY.toLowerCase()} style={{ padding: '5px' }}>
                    <Button className="icon-button" onClick={() => handleRemoveProduct(id, name)}>
                        <img src={deleteIcon} alt=""></img>
                    </Button>
                </td>
            );
        }
    };

    return (
        <><h1 id="shopping">Shopping</h1>
            <div className="app custom-table"> {message ? <div>{message}</div> :
                isLoginFormVisible ? <LoginForm onSubmit={(e) => {
                    handleLoginSubmit(e)
                }} /> :
                    isLoading ? <Spinner /> :
                        isPopupVisible ? "editProduct" === popup
                            ? <EditProductPopup content={selectedProductData}
                                onSubmit={(id, name, isRare, categoryId, category) => {
                                    setIsPopupVisible(false);
                                    updateProduct(id, name, isRare, categoryId, category)
                                }}
                                onPopupClose={() => {
                                    setSelectedProductData({});
                                    setIsPopupVisible(false);
                                }}
                                categories={sortables} />
                            : "removeProduct" === popup
                                ? <ConfirmProductRemovalPopup content={selectedProductData}
                                    onSubmit={(id, name) => {
                                        setIsPopupVisible(false);
                                        removeProduct(id, name);
                                    }}
                                    onPopupClose={() => {
                                        setSelectedProductData({});
                                        setIsPopupVisible(false);
                                    }} />
                                : <EditSortablesPopup onOrderSave={handleOrderSave}
                                    onItemMove={(fromIndex, toIndex) => {
                                        if (fromIndex === toIndex) {
                                            return;
                                        }
                                        const updatedItems = [...sortables];
                                        const [movedItem] = updatedItems.splice(fromIndex, 1);
                                        updatedItems.splice(toIndex, 0, movedItem);
                                        setSortables(updatedItems);
                                    }}
                                    onPopupClose={() => setIsPopupVisible(false)}
                                    sortables={sortables} />
                            : <>{tableData && <>
                                <Form onSubmit={(e) => handleAddProductSubmit(e)}>
                                    <Form.Control type="text" />
                                    <Button className="thirty-percent" type="submit" variant="success">Add</Button>
                                    <Button className="thirty-percent" onClick={() => setIsShowOnlyPositive((prev) => !prev)}>{
                                        isShowOnlyPositive ? "Any value" : "Hide zero"
                                    }</Button>
                                    <Button className="thirty-percent" onClick={() => setIsShowOnlyCommon((prev) => !prev)}>{
                                        isShowOnlyCommon ? "Any rarity" : "Hide rare"
                                    }</Button>
                                    <Button className="fifty-percent" onClick={() => {
                                        Object.values(inputsRef.current).forEach((input) => {
                                            if (input) {
                                                input.value = "";
                                            }
                                        });
                                        setFilters({});
                                    }}>Clear filters</Button>
                                    <Button className="fifty-percent restart" onClick={() => handleResetAll()}>Reset all</Button>
                                </Form>
                                <Table striped bordered hover responsive>
                                    <thead>
                                        <tr>
                                            {constants.META.KEYS.filter((key) => constants.META.VISIBLE[key]).map((key) => (
                                                <th key={key}>
                                                    {constants.META.FILTERABLE[key] && (
                                                        <Form.Control ref={(e) => inputsRef.current[key] = e}
                                                            type="text"
                                                            inputMode={constants.META.DATATYPE[key] === constants.NUMBER ? "numeric" : "text"}
                                                            placeholder={constants.META.DISPLAY_NAME[key]}
                                                            defaultValue={constants.META.DATATYPE[key] === constants.NUMBER && isNaN(filters[key]) ? "" : filters[key]}
                                                            onChange={(e) => setFilterValue({
                                                                column: key,
                                                                value: constants.META.DATATYPE[key] === constants.NUMBER ? parseInt(e.target.value) : e.target.value
                                                            })}
                                                            onClick={(e) => e.target.select()} />
                                                    )}
                                                    {constants.META.SORTABLE[key] && (
                                                        <Button onClick={() => { handleOrderClick(key); }}>
                                                            {(constants.CATEGORY_KEY === key && constants.CATEGORY_ORDER_KEY === order.key)
                                                                || (constants.NAME_KEY === key && constants.ORDER_KEY === order.key)
                                                                || order.key === key
                                                                ? order.order === constants.ASC ? '▲' : '▼' : 'Sort'}
                                                        </Button>
                                                    )}
                                                </th>
                                            ))}
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {applyOrder(applyFilters(tableData, filters), order).map((row) => {
                                            const id = row[constants.ID_KEY];
                                            return (
                                                <tr key={id}>
                                                    {constants.META.KEYS
                                                        .filter((key) => constants.META.VISIBLE[key])
                                                        .map((key) => renderCell(key, id, row[constants.NAME_KEY],
                                                            row[constants.IS_RARE_KEY], row[constants.CATEGORY_ID_KEY],
                                                            row[constants.CATEGORY_KEY], row[constants.QUANTITY_KEY]))}
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </Table>
                                <div className="flex-div">
                                    <Button className="fifty-percent" onClick={() => {
                                        const filteredData = tableData.map(item => ({
                                            [constants.ID_KEY]: item[constants.ID_KEY],
                                            [constants.NAME_KEY]: item[constants.NAME_KEY],
                                            [constants.ORDER_KEY]: item[constants.ORDER_KEY],
                                        }))
                                            .sort((a, b) => a[constants.ORDER_KEY] - b[constants.ORDER_KEY]);
                                        setSortables(filteredData);
                                        setSortablesType("products");
                                        showSortPopup();
                                    }}>Sort products</Button>
                                    <Button className="fifty-percent" onClick={() => {
                                        getPossibleCategories();
                                        setSortablesType("categories");
                                        showSortPopup();
                                    }}>Sort categories</Button>
                                </div>
                            </>}</>
            }</div></>
    );
};

export default Shopping;
