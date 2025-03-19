import { PropTypes } from "prop-types";
import { Button } from "react-bootstrap";
import SortableList from "../SortableList";
const EditSortablesPopup = (props) => {
    return (
        <div>
            <div className="flex-div">
                <div></div>
                <Button className="restart popup-icon" onClick={props.onPopupClose} >X</Button>
            </div>
            <div style={{ paddingRight: "20%" }}>
                <SortableList items={props.sortables} onOrderSave={props.onOrderSave} onItemMove={props.onItemMove} />
            </div>
        </div >
    );
}

EditSortablesPopup.propTypes = {
    sortables: PropTypes.array,
    onOrderSave: PropTypes.func,
    onPopupClose: PropTypes.func,
    onItemMove: PropTypes.func
};

export default EditSortablesPopup;