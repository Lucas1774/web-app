package com.lucas.server.common.jpa.user;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.mapper.EntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserJpaService extends GenericJpaServiceDelegate<User, User, UserRepository> {

    public UserJpaService(UserRepository repository, EntityMapper<User, User> mapper) {
        super(repository, mapper);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }
}
