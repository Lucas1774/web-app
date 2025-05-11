package com.lucas.server.common.exception;

import java.io.IOException;

public class JsonProcessingException extends IOException {

    public JsonProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonProcessingException(String message) {
        super(message);
    }
}
