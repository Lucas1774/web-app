package com.lucas.server.common.jpa;

public interface JpaService<E extends JpaEntity> {

    E save(E entity);

}
