import { PropTypes } from "prop-types";
import { useEffect, useRef, useState } from "react";
import { Button, Form } from "react-bootstrap";
import * as constants from "../../constants";

const EditProductPopup = (props) => {
    const [isSelectVisible, setIsSelectVisible] = useState(true);

    const nameRef = useRef(null);
    // hack to have a 2 in 1 input
    const categoryRef = useRef(null);
    const categoryRef2 = useRef(null);
    const isRareRef = useRef(null);

    useEffect(() => {
        if (props.categories.length === 0) {
            setIsSelectVisible(false);
        }
    }, [props.categories, props.content]);

    useEffect(() => {
        if (!isSelectVisible) {
            categoryRef2.current.focus();
            categoryRef2.current.select();
        }
    }, [isSelectVisible]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        const name = nameRef.current.value;
        const isRare = isRareRef.current.checked;
        let categoryId;
        let categoryName;
        if (isSelectVisible) {
            categoryId = parseInt(categoryRef.current.value);
            categoryName = props.categories.find((cat) => cat[constants.CATEGORY_ID_KEY] === categoryId)[constants.NAME_KEY];
        } else {
            categoryName = categoryRef2.current.value;
            const category = props.categories.find((cat) => cat[constants.NAME_KEY] === categoryName);
            categoryId = category ? category[constants.CATEGORY_ID_KEY] : null;
        }
        if (name && categoryName) {
            props.onSubmit(props.content[constants.ID_KEY], name, isRare, categoryId, categoryName);
        }
    };

    return (
        <div>
            <div className="popup-header">
                <div></div>
                <Button className="restart popup-icon" onClick={props.onPopupClose} >X</Button>
            </div>
            <div>
                <Form onSubmit={(e) => handleSubmit(e)}>
                    <Form.Label>Name:</Form.Label>
                    <Form.Control defaultValue={props.content[constants.NAME_KEY]} ref={nameRef} />
                    <Form.Label>Category:</Form.Label>
                    {isSelectVisible
                        ? <Form.Control as="select"
                            ref={categoryRef}
                            defaultValue={props.content[constants.CATEGORY_KEY][constants.CATEGORY_ID_KEY]}
                            onChange={() => {
                                if (categoryRef.current.value === constants.NEW) {
                                    setIsSelectVisible(false);
                                }
                            }}>
                            {props.categories.map((option) => (
                                <option key={option[constants.CATEGORY_ID_KEY]} value={option[constants.CATEGORY_ID_KEY]}>{option[constants.NAME_KEY]}</option>
                            ))}
                            <option key={null} value={constants.NEW}>New</option>
                        </Form.Control>
                        : <Form.Control
                            type="text" ref={categoryRef2} defaultValue={"Create a new category"}
                        />}
                    <Form.Check
                        type="checkbox" ref={isRareRef} label="Is Rare"
                        defaultChecked={props.content[constants.IS_RARE_KEY]}
                    />
                    <Button type="submit" variant="success">Submit</Button>
                </Form>
            </div>
        </div>
    );
};

EditProductPopup.propTypes = {
    content: PropTypes.object,
    onSubmit: PropTypes.func,
    onPopupClose: PropTypes.func,
    categories: PropTypes.array
};

export default EditProductPopup;
