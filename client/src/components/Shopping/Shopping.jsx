import React, { useCallback, useEffect, useRef, useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import { get, post } from "../../api";
import deleteIcon from "../../assets/images/bin.png";
import editIcon from "../../assets/images/edit.png";
import resetIcon from "../../assets/images/remove.png";
import * as constants from "../../constants";
import useDebounce from "../../hooks/useDebounce";
import { handleError } from "../errorHandler";
import LoginForm from "../LoginForm";
import Spinner from "../Spinner";
import ConfirmProductRemovalPopup from "./ConfirmProductRemovalPopup";
import EditCategoriesPopup from "./EditCategoriesPopup";
import EditProductPopup from "./EditProductPopup";
import "./Shopping.css";

const Shopping = () => {
    const [tableData, setTableData] = useState(null);
    const [categories, setCategories] = useState([]);
    const [popup, setPopup] = useState(null);
    const [quantityInputValue, setQuantityInputValue] = useState({});
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
    const debouncedValue = useDebounce(quantityInputValue, 1000);
    const filterDebouncedValue = useDebounce(filterValue, 1000)

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
                const response = await get("/check-auth");
                if (response.data !== 1) {
                    setIsLoginFormVisible(true);
                } else {
                    await getData();
                }
            } catch (error) {
                handleError("Error checking authentication", error);
            } finally {
                setIsLoading(false);
            }
        };

        checkAuth();
    }, []);

    const getData = async () => {
        setIsLoading(true);
        try {
            const response = await get("/shopping");
            setTableData(response.data);
        } catch (error) {
            handleError("Error fetching data", error);
        } finally {
            setIsLoading(false);
        }
    };

    const makeGenericRequest = async (request, callbackAfterSuccess) => {
        setIsLoading(true);
        let message = "";
        try {
            const response = await request();
            message = response.data;
        } catch (error) {
            handleError("Error sending data", error);
        } finally {
            if (message === null) {
                setIsLoading(false);
            } else {
                if (message) {
                    setMessage(message);
                    setTimeout(() => {
                        setMessage(null);
                        callbackAfterSuccess();
                    }, constants.TIMEOUT_DELAY);
                } else {
                    callbackAfterSuccess();
                }
            }
        }
    };

    const updateProductQuantity = useCallback(async (value, id, name) => {
        if (isNaN(value) || parseInt(value) < 0) {
            return;
        }
        makeGenericRequest(() => post('/update-product-quantity', { [constants.ID_KEY]: id, [constants.NAME_KEY]: name, [constants.QUANTITY_KEY]: parseInt(value) }), () => {
            setIsLoading(true);
            getData();
        });
    }, []);

    const updateProduct = async (id, name, isRare, categoryId, category) => {
        makeGenericRequest(() => post('/update-product', {
            [constants.ID_KEY]: id, [constants.NAME_KEY]: name,
            [constants.IS_RARE_KEY]: isRare, [constants.CATEGORY_ID_KEY]: categoryId, [constants.CATEGORY_KEY]: category
        }), () => {
            setIsLoading(true);
            setIsPopupVisible(false);
            getData();
        });
    };

    const removeProduct = async (id, name) => {
        makeGenericRequest(() => post('/remove-product', { [constants.ID_KEY]: id, [constants.NAME_KEY]: name }), () => {
            setIsLoading(true);
            getData();
        });
    };

    useEffect(() => {
        if (debouncedValue?.value != null && debouncedValue?.rowId != null && debouncedValue?.rowName != null) {
            updateProductQuantity(debouncedValue.value, debouncedValue.rowId, debouncedValue.rowName);
        }
    }, [debouncedValue, updateProductQuantity]);

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
            makeGenericRequest(() => post('/login', { [constants.USERNAME]: username, [constants.PASSWORD]: password }), () => {
                setIsLoading(true);
                setIsLoginFormVisible(false);
                getData();
            });
        }
    };

    const handleResetAll = async () => {
        makeGenericRequest(() => post('/update-all-product-quantity', null), () => {
            setIsLoading(true);
            getData();
        });
    };

    const handleAddProductSubmit = async (event) => {
        event.preventDefault();
        const name = event.target[0].value.trim();
        if (!name) {
            return;
        }
        makeGenericRequest(() => post('/new-product', name), () => {
            setIsLoading(true);
            getData();
        });
    };

    const getPossibleCategories = async () => {
        setIsLoading(true);
        try {
            const response = await get("/get-possible-categories");
            setCategories(response.data);
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

    const handleEditCategories = () => {
        getPossibleCategories();
        setPopup("editCategories");
        setIsPopupVisible(true);
    };

    const handleRemoveProduct = async (id, name) => {
        setSelectedProductData({ [constants.ID_KEY]: id, [constants.NAME_KEY]: name });
        setPopup("removeProduct");
        setIsPopupVisible(true);
    };

    const handleOrderSave = async () => {
        categories.forEach((category, index) => {
            category[constants.CATEGORY_ORDER_KEY] = index + 1;
        });
        makeGenericRequest(() => post('/update-categories', categories), () => {
            setIsLoading(true);
            setIsPopupVisible(false);
            getData();
        });
    };

    const handleOrderClick = (key) => {
        const actualKey = key === constants.CATEGORY_KEY ? constants.CATEGORY_ORDER_KEY : key;
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

    const renderColumn = (key, id, name, isRare, categoryId, category, quantity) => {
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
                        <Form.Control defaultValue={quantity}
                            inputMode="numeric"
                            onChange={(e) =>
                                setQuantityInputValue({
                                    value: parseInt(e.target.value),
                                    rowId: id,
                                    rowName: name
                                })}
                            onClick={(e) => e.target.select()}
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
            <div className="app shopping"> {message ? <div>{message}</div> :
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
                                categories={categories} />
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
                                : <EditCategoriesPopup onOrderSave={handleOrderSave}
                                    onItemMove={(fromIndex, toIndex) => {
                                        if (fromIndex === toIndex) {
                                            return;
                                        }
                                        const updatedItems = [...categories];
                                        const [movedItem] = updatedItems.splice(fromIndex, 1);
                                        updatedItems.splice(toIndex, 0, movedItem);
                                        setCategories(updatedItems);
                                    }}
                                    onPopupClose={() => setIsPopupVisible(false)}
                                    categories={categories} />
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
                                                            {(constants.CATEGORY_KEY === key && constants.CATEGORY_ORDER_KEY === order.key) || order.key === key
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
                                                        .map((key) => renderColumn(key, id, row[constants.NAME_KEY],
                                                            row[constants.IS_RARE_KEY], row[constants.CATEGORY_ID_KEY],
                                                            row[constants.CATEGORY_KEY], row[constants.QUANTITY_KEY]))}
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </Table>
                                <Button onClick={handleEditCategories}>Sort categories</Button>
                            </>}</>
            }</div></>
    );
};

export default Shopping;
