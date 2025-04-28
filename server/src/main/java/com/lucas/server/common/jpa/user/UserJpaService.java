package com.lucas.server.common.jpa.user;

import com.lucas.server.common.jpa.JpaService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserJpaService implements JpaService<User> {

    private final UserRepository repository;

    public UserJpaService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> save(User entity) {
        return Optional.of(this.repository.save(entity));
    }

    @Override
    public List<User> saveAll(Iterable<User> entities) {
        return this.repository.saveAll(entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public List<User> findAll() {
        return this.repository.findAll();
    }

    @Override
    public Optional<User> findById(String id) {
        return this.repository.findById(Long.valueOf(id));
    }

    public Optional<User> findByUsername(String username) {
        return this.repository.findByUsername(username);
    }
}
