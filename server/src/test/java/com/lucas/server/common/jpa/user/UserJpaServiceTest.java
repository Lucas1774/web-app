package com.lucas.server.common.jpa.user;

import com.lucas.server.TestcontainersConfiguration;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserJpaServiceTest {

    @Autowired
    private UserJpaService userService;

    @Test
    @Transactional
    void testUserPasswordFlow() {
        // given
        User user = new User()
                .setUsername("alice")
                .setPassword("secret");
        userService.createAll(Collections.singletonList(user));

        // when & then
        assertThat(userService.findByUsername("alice").orElseThrow().getPassword()).isEqualTo("secret");
        assertThat(userService.findByUsername("bob")).isEmpty();
    }
}
