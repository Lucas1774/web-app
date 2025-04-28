package com.lucas.server.common.jpa.user;

import com.lucas.server.TestcontainersConfiguration;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserJpaServiceTest {

    @Autowired
    UserJpaService userJpaService;

    @Test
    void testUserPasswordFlow() {
        // given
        User user = new User()
                .setUsername("alice")
                .setPassword("secret");
        this.userJpaService.save(user);

        // when & then
        assertThat(this.userJpaService.findByUsername("alice").map(User::getPassword)).contains("secret");
        assertThat(this.userJpaService.findByUsername("bob").map(User::getPassword)).isEmpty();
    }
}
