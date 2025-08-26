package com.lucas.server.common;

import com.lucas.server.common.exception.JsonProcessingException;

public interface Mapper<K, V> {

    @SuppressWarnings("unused")
    V map(K key) throws JsonProcessingException;
}
