package com.lucas.server;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Import(TestConfiguration.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class ConfiguredTest extends BaseTest {
}
