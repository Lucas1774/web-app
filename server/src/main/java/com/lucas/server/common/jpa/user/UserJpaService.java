package com.lucas.server.common.jpa.user;

import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.jpa.JpaService;
import com.lucas.server.common.mapper.IdentityEntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class UserJpaService implements JpaService<User> {

    private final GenericJpaServiceDelegate<User, User, UserRepository> delegate;
    private final UserRepository repository;

    public UserJpaService(UserRepository repository, IdentityEntityMapper<User> identityEntityMapper) {
        delegate = new GenericJpaServiceDelegate<>(repository, identityEntityMapper);
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    @Transactional
    public Set<User> saveAll(Set<User> elements) {
        return delegate.saveAll(elements);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<User> findAll() {
        return delegate.findAll();
    }

    @Override
    @Transactional
    public void deleteAll(Set<User> elements) {
        delegate.deleteAll(elements);
    }
}
