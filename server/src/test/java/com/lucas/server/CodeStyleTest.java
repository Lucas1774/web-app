package com.lucas.server;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
class CodeStyleTest {

    @Test
    void testCodeStyle() throws CheckstyleException, IOException {
        String configPath = Paths.get("")
                .toAbsolutePath()
                .resolve("checkstyle.xml")
                .toString();

        Configuration config =
                ConfigurationLoader.loadConfiguration(configPath, new PropertiesExpander(new Properties()));

        Checker checker = new Checker();
        checker.setModuleClassLoader(Checker.class.getClassLoader());
        checker.configure(config);

        // Collect violations
        List<String> violations = new ArrayList<>();
        AuditListener listener = new AuditListener() {
            @Override
            public void auditStarted(AuditEvent event) {
            }

            @Override
            public void auditFinished(AuditEvent event) {
            }

            @Override
            public void fileStarted(AuditEvent event) {
            }

            @Override
            public void fileFinished(AuditEvent event) {
            }

            @Override
            public void addError(AuditEvent event) {
                violations.add(String.format("%s:%d: %s", event.getFileName(), event.getLine(), event.getMessage()));
            }

            @Override
            public void addException(AuditEvent event, Throwable throwable) {
                violations.add(String.format("%s: %s", event.getFileName(), throwable.getMessage()));
            }
        };

        checker.addListener(listener);

        // Recursively find all .java files from both main and test sources
        List<File> javaFiles = new ArrayList<>();
        String[] sourcePaths = {"src/main/java", "src/test/java"};

        for (String sourcePathStr : sourcePaths) {
            Path sourcePath = Paths.get(sourcePathStr);
            if (Files.exists(sourcePath)) {
                try (Stream<Path> walk = Files.walk(sourcePath)) {
                    walk.filter(Files::isRegularFile)
                            .filter(p -> p.toString()
                                    .endsWith(".java"))
                            .forEach(p -> javaFiles.add(p.toFile()));
                }
            }
        }

        if (!javaFiles.isEmpty()) {
            checker.process(javaFiles);
        }

        checker.destroy();

        if (!violations.isEmpty()) {
            StringBuilder message = new StringBuilder("Checkstyle violations found:\n");
            violations.forEach(v -> message.append("  - ")
                    .append(v)
                    .append("\n"));
            assertEquals(0, violations.size(), message.toString());
        }
    }
}
