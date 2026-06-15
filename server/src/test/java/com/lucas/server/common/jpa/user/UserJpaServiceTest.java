package com.lucas.server.common.jpa.user;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.common.dto.user.UserDomain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserJpaServiceTest extends ConfiguredTest {

    @Autowired
    private UserJpaService userService;

    @Test
    void userPasswordFlow() {
        // given
        UserDomain user = new UserDomain().setUsername("alice").setPassword("secret");
        userService.saveAll(Set.of(user));

        // when & then
        assertThat(userService.findByUsername("alice").orElseThrow().getPassword()).isEqualTo("secret");
        assertThat(userService.findByUsername("bob")).isEmpty();
    }
}
