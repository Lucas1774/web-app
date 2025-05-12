package com.lucas.server.common.jpa.user;

import com.lucas.server.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
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
    UserJpaService userService;

    @BeforeEach
    void setUp() {
        this.userService.deleteAll();
    }

    @Test
    void testUserPasswordFlow() {
        // given
        User user = new User()
                .setUsername("alice")
                .setPassword("secret");
        this.userService.createAll(Collections.singletonList(user));

        // when & then
        assertThat(this.userService.findByUsername("alice").orElseThrow().getPassword()).isEqualTo("secret");
        assertThat(this.userService.findByUsername("bob")).isEmpty();
    }
}
