import { PropTypes } from "prop-types";
import { Button } from "react-bootstrap";

const RecommendationPopup = ({ content, onPopupClose }) => {
    const { rationale, news } = content;

    return (
        <div>
            <div className="flex-div">
                <div></div>
                <Button className="restart popup-icon" onClick={onPopupClose}>X</Button>
            </div>

            <h4>Rationale</h4><p>{rationale}</p>

            <h4>Related News</h4><ul>
                {news.sort((a, b) => new Date(b.date) - new Date(a.date))
                    .map((n) => (
                        <li key={n.id}>
                            <strong>{new Date(n.date).toLocaleString()}:</strong> {n.headline}
                            <p>{n.summary}</p>
                            {n.sentiment && (
                                <p>Sentiment: {n.sentiment} ({n.sentimentConfidence ?? "N/A"})</p>
                            )}
                        </li>
                    ))}
            </ul>
        </div>
    );
};

RecommendationPopup.propTypes = {
    content: PropTypes.shape({
        rationale: PropTypes.string,
        news: PropTypes.arrayOf(
            PropTypes.shape({
                id: PropTypes.number,
                date: PropTypes.string,
                headline: PropTypes.string,
                summary: PropTypes.string,
                sentiment: PropTypes.string,
                sentimentConfidence: PropTypes.number
            })
        )
    }),
    onPopupClose: PropTypes.func
};

export default RecommendationPopup;
