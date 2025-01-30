import { PropTypes } from "prop-types";
import { Button, Form } from "react-bootstrap";
import * as constants from "../../constants";

const ConfirmProductRemovalPopup = (props) => {

    const handleSubmit = async (e) => {
        e.preventDefault();
        props.onSubmit(props.content[constants.ID_KEY], props.content[constants.NAME_KEY]);
    };

    return (
        <div>
            <div className="popup-header">
                <div></div>
                <Button className="restart popup-icon" onClick={props.onPopupClose} >X</Button>
            </div>
            <div>
                <Form onSubmit={handleSubmit}>
                    <Button className="restart" onClick={handleSubmit}>Confirm deletion</Button>
                </Form>
            </div>
        </div>
    );
};

ConfirmProductRemovalPopup.propTypes = {
    content: PropTypes.object,
    onSubmit: PropTypes.func,
    onPopupClose: PropTypes.func,
};

export default ConfirmProductRemovalPopup;
