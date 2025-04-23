package com.lucas.server.common;

public interface Mapper<K, V> {

    V map(K key) throws JsonProcessingException;

}
