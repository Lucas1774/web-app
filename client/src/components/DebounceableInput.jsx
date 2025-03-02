import { PropTypes } from 'prop-types';
import React, { useEffect, useState } from 'react';
import Form from 'react-bootstrap/Form';
import { DEBOUNCE_DELAY } from '../constants';
import useDebounce from '../hooks/useDebounce';

const DebounceableInput = ({ value, id, name, onDebouncedChange }) => {
  const [innerValue, setInnerValue] = useState(value);
  const [shouldListenToDebounce, setShouldListenToDebounce] = useState(false);
  const debouncedValue = useDebounce(innerValue, DEBOUNCE_DELAY);

  // do not mind debouncedValue changes if innerValue changes come from outside
  useEffect(() => {
    setInnerValue(value);
    setShouldListenToDebounce(false);
  }, [value]);

  useEffect(() => {
    if (shouldListenToDebounce && debouncedValue !== null && debouncedValue !== value) {
      onDebouncedChange(debouncedValue, id, name);
    }
  }, [debouncedValue, id, name, onDebouncedChange, shouldListenToDebounce, value]);

  return (
    <Form.Control
      value={innerValue}
      inputMode='numeric'
      onChange={(e) => {
        if (isNaN(e.target.value) || parseInt(e.target.value) < 0) {
          return;
        }
        setInnerValue(e.target.value === "" ? 0 : parseInt(e.target.value));
        setShouldListenToDebounce(true);
      }}
      onClick={(e) => e.target.select()}
    />
  );
};

DebounceableInput.propTypes = {
  value: PropTypes.number.isRequired,
  id: PropTypes.number.isRequired,
  name: PropTypes.string.isRequired,
  onDebouncedChange: PropTypes.func,
};

export default DebounceableInput;
