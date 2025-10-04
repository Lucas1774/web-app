import { PropTypes } from "prop-types";
import { useRef } from "react";
import { Button } from "react-bootstrap";

const FileImporter = ({ onFileContentChange }) => {
    const fileInput = useRef(null);

    const handleFileChange = (event) => {
        const file = event.target.files[0];
        const fileReader = new FileReader();
        fileReader.onloadend = () => {
            const content = fileReader.result;
            const stringifiedContent = JSON.stringify(content);
            onFileContentChange(stringifiedContent);
        };
        fileReader.readAsText(file);
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
