package com.lucas.server;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class DotEnvSystemPropertyListener implements TestExecutionListener {

    @Override
    public void beforeTestClass(@NotNull TestContext testContext) throws Exception {
        Path dotEnv = Path.of(System.getProperty("user.dir"), ".env");
        if (Files.exists(dotEnv)) {
            try (Stream<String> lines = Files.lines(dotEnv)) {
                lines.map(String::trim)
                        .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                        .forEach(l -> {
                            int eq = l.indexOf('=');
                            if (0 < eq) {
                                System.setProperty(l.substring(0, eq).trim(), l.substring(eq + 1).trim()
                                );
                            }
                        });
            }
        }
    }
}
