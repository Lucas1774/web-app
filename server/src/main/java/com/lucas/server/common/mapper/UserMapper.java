package com.lucas.server.common.mapper;

import com.lucas.server.common.dto.user.UserDomain;
import com.lucas.server.common.jpa.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper implements EntityMapper<User, UserDomain> {

    @Override
    public UserDomain toDto(User entity) {
        if (null == entity) {
            return null;
        }
        return new UserDomain(entity.getId(), entity.getUsername(), entity.getPassword());
    }

    @Override
    public User toEntity(UserDomain dto) {
        if (null == dto) {
            return null;
        }
        return new User().setId(dto.getId()).setUsername(dto.getUsername()).setPassword(dto.getPassword());
    }
}
