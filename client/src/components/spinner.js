import { PropTypes } from 'prop-types'
const Spinner = ({ color = "#fff", position = "relative" }) => {
    return <svg xmlns="http://www.w3.org/2000/svg"
        width="40"
        height="40"
        viewBox="0 0 40 40"
        stroke={color}
        fill={color}
        style={{
            display: "block", margin: "auto", position: position, ...(position === "absolute" && {
                alignSelf: "center",
                left: "50%",
                transform: "translateX(-50%)"
            })
        }}>
        <g fill="none" fillRule="evenodd">
            <g transform="translate(2 2)" strokeWidth="4">
                <circle strokeOpacity=".5" cx="18" cy="18" r="18" />
                <path d="M36 18c0-9.94-8.06-18-18-18">
                    <animateTransform
                        attributeName="transform"
                        type="rotate"
                        from="0 18 18"
                        to="360 18 18"
                        dur="1s"
                        repeatCount="indefinite"
                    />
                </path>
            </g>
        </g>
    </svg>
};

Spinner.propTypes = {
    color: PropTypes.string,
    position: PropTypes.string
};

export default Spinner;
