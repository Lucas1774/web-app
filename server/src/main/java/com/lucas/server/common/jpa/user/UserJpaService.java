package com.lucas.server.common.jpa.user;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserJpaService implements JpaService<User> {

    @Delegate
    private final GenericJpaServiceDelegate<User, UserRepository> delegate;
    private final UserRepository repository;

    public UserJpaService(UserRepository repository) {
        delegate = new GenericJpaServiceDelegate<>(repository);
        this.repository = repository;
    }

    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }
}
