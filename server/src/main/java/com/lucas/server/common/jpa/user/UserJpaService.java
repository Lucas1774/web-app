package com.lucas.server.common.jpa.user;

import com.lucas.server.common.dto.user.UserDomain;
import com.lucas.server.common.jpa.GenericJpaServiceDelegate;
import com.lucas.server.common.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserJpaService extends GenericJpaServiceDelegate<User, UserDomain, UserRepository> {

    public UserJpaService(UserRepository repository, UserMapper mapper) {
        super(repository, mapper);
    }

    @Transactional(readOnly = true)
    public Optional<UserDomain> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDto);
    }
}
