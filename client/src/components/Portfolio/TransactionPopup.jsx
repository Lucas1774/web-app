import PropTypes from "prop-types";
import { useState } from "react";
import { Button, Form } from "react-bootstrap";
import { post } from "../../api";
import { TIMEOUT_DELAY } from "../../constants";
import { handleError } from "../errorHandler";
import Spinner from "../Spinner";
import "./Portfolio.css";

const TransactionPopup = ({ id: symbolId, name: symbolName, onPopupClose, onTransactionSuccessful }) => {
    const [action, setAction] = useState("");
    const [price, setPrice] = useState("");
    const [quantity, setQuantity] = useState("");
    const [commission, setCommission] = useState("");
    const [message, setMessage] = useState(null);
    const [isLoading, setIsLoading] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!price || !quantity || (action === "buy" && !commission)) {
            setMessage("Specify a commission");
            setTimeout(() => {
                setMessage(null);
            }, TIMEOUT_DELAY)
            return;
        }
        setIsLoading(true);
        try {
            const p = parseFloat(price);
            const q = parseFloat(quantity);
            const c = parseFloat(commission);
            // IBKR idiosyncrasies
            const priceWithCommission = p + (c / q);
            const relativeCommission = c / (p * q);
            const commissionParam = action === "buy" ? `&commission=${relativeCommission}` : "";
            await post(`/portfolio/${action}?symbolId=${symbolId}&price=${priceWithCommission}&quantity=${q}` + commissionParam, "");
            setMessage(`${action} ${symbolName} successful`);
            setTimeout(() => {
                setMessage(null);
                onPopupClose();
                onTransactionSuccessful();
            }, TIMEOUT_DELAY);
        } catch (error) {
            if (error.response?.status === 401) {
                setMessage("Unauthorized");
                setTimeout(() => {
                    setMessage(null);
                }, TIMEOUT_DELAY);
            } else if (error.response?.status === 422) {
                setMessage("Nothing to sell");
                setTimeout(() => {
                    setMessage(null);
                }, TIMEOUT_DELAY);
            } else {
                handleError("Error fetching data", error);
            }
        } finally {
            setIsLoading(false);
        }
    };

    if (isLoading) {
        return <Spinner />;
    }
    if (message) {
        return <div>{message}</div>;
    }

    return (
        <div>
            <div className="flex-div">
                <div></div>
                <Button className="restart popup-icon" onClick={onPopupClose}>X</Button>
            </div>

            {
                <div className="app portfolio">
                    <h4>Registering action for {symbolName}</h4>
                    <Form onSubmit={handleSubmit}>
                        <Button type="submit" className="fifty-percent" variant="success" onClick={() => setAction("buy")}>Buy</Button>
                        <Button type="submit" className="restart fifty-percent" onClick={() => setAction("sell")}>Sell</Button>
                        <Form.Control style={{ width: "100%", margin: "5px" }}
                            type="number"
                            placeholder="Price (no commissions)"
                            min="0.01"
                            step="0.01"
                            value={price}
                            onChange={(e) => setPrice(e.target.value)}
                            required />
                        <Form.Control style={{ width: "100%", margin: "5px" }}
                            type="number"
                            placeholder="Quantity"
                            min="0.0001"
                            step="0.0001"
                            value={quantity}
                            onChange={(e) => setQuantity(e.target.value)}
                            required />
                        <Form.Control style={{ width: "100%", margin: "5px" }}
                            type="number"
                            placeholder="Commission (total, round-trip flat)"
                            step="0.0001"
                            value={commission}
                            onChange={(e) => setCommission(e.target.value)} />
                    </Form>
                </ div >
            }
        </div>
    );
};

TransactionPopup.propTypes = {
    id: PropTypes.number,
    name: PropTypes.string,
    onPopupClose: PropTypes.func,
    onTransactionSuccessful: PropTypes.func
};

export default TransactionPopup;
