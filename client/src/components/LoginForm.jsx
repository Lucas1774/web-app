import { PropTypes } from 'prop-types';
import { Button, Form } from 'react-bootstrap';

const LoginForm = ({ onSubmit }) => {

    return (
        <Form onSubmit={onSubmit}>
            <Form.Control placeholder='username' />
            <Form.Control placeholder='password' type="password" />
            <Button className="fifty-percent" type="submit" variant="success" value="validate">Validate</Button>
            <Button className="fifty-percent" type="submit" value="continue">Use as Guest</Button>
        </Form>
    );
};

LoginForm.propTypes = {
    onSubmit: PropTypes.func,
};

export default LoginForm;
