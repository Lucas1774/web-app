package com.lucas.server.common;

import java.io.IOException;

public class JsonProcessingException extends IOException {

    public JsonProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

}
