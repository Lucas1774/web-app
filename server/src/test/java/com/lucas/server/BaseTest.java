package com.lucas.server;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestExecutionListeners(listeners = DotEnvSystemPropertyListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = "spring.jpa.show-sql=false")
public abstract class BaseTest {
}
