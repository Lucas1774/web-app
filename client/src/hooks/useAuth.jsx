import { useCallback, useEffect, useRef } from 'react';
import { get, post } from '../api';
import { handleError } from '../components/errorHandler';
import * as constants from '../constants';

const useAuth = ({ onAuthSuccess, onGuest, setMessage: setExternalMessage, setLoading: setExternalLoading, setLoginFormVisible: setExternalLoginFormVisible } = {}) => {
    const onAuthSuccessRef = useRef(onAuthSuccess);
    const onGuestRef = useRef(onGuest);

    const setMessageRef = useRef(setExternalMessage);
    const setLoadingRef = useRef(setExternalLoading);
    const setLoginFormVisibleRef = useRef(setExternalLoginFormVisible);

    useEffect(() => {
        onAuthSuccessRef.current = onAuthSuccess;
        onGuestRef.current = onGuest;
        setMessageRef.current = setExternalMessage;
        setLoadingRef.current = setExternalLoading;
        setLoginFormVisibleRef.current = setExternalLoginFormVisible;
    }, [onAuthSuccess, onGuest, setExternalLoading, setExternalLoginFormVisible, setExternalMessage]);

    const checkAuth = useCallback(async () => {
        setLoadingRef.current(true);
        try {
            await get('/authentication/check-auth');
            if (onAuthSuccessRef.current) {
                await onAuthSuccessRef.current();
            }
        } catch (error) {
            if (error?.response?.status === 403) {
                setLoginFormVisibleRef.current(true);
            } else {
                handleError('Error checking authentication', error);
            }
        } finally {
            setLoadingRef.current(false);
        }
    }, []);

    useEffect(() => {
        checkAuth();
    }, [checkAuth]);

    const handleLoginSubmit = async (event) => {
        event.preventDefault();
        const username = event.target[0].value.trim();
        const password = event.target[1].value.trim();
        const action = event.nativeEvent.submitter.value;
        if (!password || 'validate' !== action) {
            setMessageRef.current('No password provided. Continuing as guest');
            setTimeout(async () => {
                setMessageRef.current(null);
                setLoginFormVisibleRef.current(false);
                if (onGuestRef.current) {
                    await onGuestRef.current();
                }
            }, constants.TIMEOUT_DELAY);
            return;
        }

        setLoadingRef.current(true);
        try {
            await post('/authentication/login', { [constants.USERNAME]: username, [constants.PASSWORD]: password });
            setMessageRef.current('Login successful');
            setTimeout(async () => {
                setMessageRef.current(null);
                setLoginFormVisibleRef.current(false);
                if (onAuthSuccessRef.current) {
                    await onAuthSuccessRef.current();
                }
            }, constants.TIMEOUT_DELAY);
        } catch (error) {
            if (error?.response?.status === 403) {
                setMessageRef.current('Wrong credentials. Continuing as guest');
                setTimeout(async () => {
                    setMessageRef.current(null);
                    setLoginFormVisibleRef.current(false);
                    if (onGuestRef.current) {
                        await onGuestRef.current();
                    }
                }, constants.TIMEOUT_DELAY);
            } else {
                handleError('Error sending data', error);
            }
        } finally {
            setLoadingRef.current(false);
        }
    };

    return handleLoginSubmit;
};

export default useAuth;
