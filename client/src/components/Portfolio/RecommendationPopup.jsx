import { PropTypes } from "prop-types";
import { Button } from "react-bootstrap";

const RecommendationPopup = ({ content, onPopupClose }) => {
    return (
        <div>
            <div className="flex-div">
                <div></div>
                <Button className="restart popup-icon"
                    onClick={onPopupClose}>X
                </Button>
            </div>
            {content}
        </div >
    );
};

RecommendationPopup.propTypes = {
    content: PropTypes.object,
    onPopupClose: PropTypes.func
};

export default RecommendationPopup;
