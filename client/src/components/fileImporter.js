import { PropTypes } from "prop-types";
import { useRef } from "react";
import { Button } from "react-bootstrap";

const FileImporter = ({ onFileContentChange }) => {
    const fileInput = useRef(null);

    const handleFileChange = async (event) => {
        const file = event.target.files[0];
        const content = await file.text();
        const stringifiedContent = JSON.stringify(content);
        onFileContentChange(stringifiedContent);
    };

    return (
        <>
            <Button onClick={() => fileInput.current.click()}>Choose File</Button>
            <input ref={fileInput} id="file-input" type="file" accept=".txt" onChange={handleFileChange} />
        </>
    );
};

FileImporter.propTypes = {
    onFileContentChange: PropTypes.func,
};

export default FileImporter;
