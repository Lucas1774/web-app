import PropTypes from 'prop-types';
import { Dropdown, Form } from 'react-bootstrap';

const MultiSelectDdl = ({ title, options, selectedOptions, setSelectedOptions }) => {
    return (
        <Dropdown>
            <Dropdown.Toggle as="span" style={{ cursor: "pointer" }}>{title}</Dropdown.Toggle>
            <Dropdown.Menu style={{ maxHeight: '200px', overflowY: 'auto' }}>
                {options.map(item => (
                    <Form.Check
                        key={item}
                        type="checkbox"
                        label={item}
                        checked={selectedOptions.includes(item)}
                        onChange={e => {
                            setSelectedOptions(prev => {
                                const next = new Set(prev);
                                if (e.target.checked) {
                                    next.add(item);
                                } else {
                                    next.delete(item);
                                }
                                return Array.from(next);
                            });
                        }}
                    />
                ))}
            </Dropdown.Menu>
        </Dropdown>
    );
};

MultiSelectDdl.propTypes = {
    title: PropTypes.string,
    options: PropTypes.arrayOf(PropTypes.string),
    selectedOptions: PropTypes.arrayOf(PropTypes.string),
    setSelectedOptions: PropTypes.func
};

export default MultiSelectDdl;
