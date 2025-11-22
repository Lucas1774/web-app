package com.lucas.server;

import org.springframework.context.annotation.Import;

@Import(TestConfiguration.class)
public abstract class ConfiguredTest extends BaseTest {
}
