package com.github.sonarqube.ai.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ProcessCliExecutor
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.3)
 */
class ProcessCliExecutorTest {

    @Test
    void testBuilderWithMinimalConfig() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .build();

        assertThat(executor.getCliPath()).isEqualTo("/usr/bin/test");
        assertThat(executor.getTimeoutSeconds()).isEqualTo(60);
        assertThat(executor.getDefaultArgs()).isEmpty();
    }

    @Test
    void testBuilderWithFullConfig() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .timeout(120)
            .addDefaultArg("--verbose")
            .addDefaultArg("--json")
            .versionArgs("-v")
            .build();

        assertThat(executor.getCliPath()).isEqualTo("/usr/bin/test");
        assertThat(executor.getTimeoutSeconds()).isEqualTo(120);
        assertThat(executor.getDefaultArgs()).containsExactly("--verbose", "--json");
    }

    @Test
    void testBuilderWithDefaultArgs() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .defaultArgs(Arrays.asList("--arg1", "--arg2"))
            .build();

        assertThat(executor.getDefaultArgs()).containsExactly("--arg1", "--arg2");
    }

    @Test
    void testBuilderRequiresCliPath() {
        assertThatThrownBy(() ->
            ProcessCliExecutor.builder().build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("CLI path is required");
    }

    @Test
    void testBuildCommandWithNoArgs() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .build();

        String[] command = executor.buildCommand();

        assertThat(command).containsExactly("/usr/bin/test");
    }

    @Test
    void testBuildCommandWithDefaultArgs() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .addDefaultArg("--verbose")
            .addDefaultArg("--json")
            .build();

        String[] command = executor.buildCommand();

        assertThat(command).containsExactly("/usr/bin/test", "--verbose", "--json");
    }

    @Test
    void testBuildCommandWithUserArgs() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .build();

        String[] command = executor.buildCommand("arg1", "arg2");

        assertThat(command).containsExactly("/usr/bin/test", "arg1", "arg2");
    }

    @Test
    void testBuildCommandWithBothDefaultAndUserArgs() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .addDefaultArg("--verbose")
            .build();

        String[] command = executor.buildCommand("analyze", "file.txt");

        assertThat(command).containsExactly("/usr/bin/test", "--verbose", "analyze", "file.txt");
    }

    @Test
    void testBuildCommandWithList() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .build();

        String[] command = executor.buildCommand(Arrays.asList("arg1", "arg2"));

        assertThat(command).containsExactly("/usr/bin/test", "arg1", "arg2");
    }

    @Test
    void testAddDefaultArg() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .build();

        assertThat(executor.getDefaultArgs()).isEmpty();

        executor.addDefaultArg("--new-arg");

        assertThat(executor.getDefaultArgs()).containsExactly("--new-arg");
    }

    @Test
    void testClearDefaultArgs() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .addDefaultArg("--arg1")
            .addDefaultArg("--arg2")
            .build();

        assertThat(executor.getDefaultArgs()).hasSize(2);

        executor.clearDefaultArgs();

        assertThat(executor.getDefaultArgs()).isEmpty();
    }

    @Test
    void testSetTimeout() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .build();

        assertThat(executor.getTimeoutSeconds()).isEqualTo(60);

        executor.setTimeoutSeconds(120);

        assertThat(executor.getTimeoutSeconds()).isEqualTo(120);
    }

    @Test
    void testSetTimeoutRejectsNegativeValue() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .build();

        assertThatThrownBy(() -> executor.setTimeoutSeconds(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Timeout must be positive");
    }

    @Test
    void testSetTimeoutRejectsZero() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/usr/bin/test")
            .build();

        assertThatThrownBy(() -> executor.setTimeoutSeconds(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Timeout must be positive");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testExecuteCommandOnWindows() throws Exception {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("cmd.exe")
            .build();

        String[] command = executor.buildCommand("/c", "echo", "Hello");
        String output = executor.executeCommand(command, null);

        assertThat(output).contains("Hello");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testExecuteCommandOnUnix() throws Exception {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/bin/echo")
            .build();

        String[] command = executor.buildCommand("Hello");
        String output = executor.executeCommand(command, null);

        assertThat(output).contains("Hello");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testIsCliAvailableOnWindows() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("cmd.exe")
            .versionArgs("/c", "ver")
            .build();

        assertThat(executor.isCliAvailable()).isTrue();
    }

    @Test
    void testIsCliAvailableWithNonexistentPath() {
        ProcessCliExecutor executor = ProcessCliExecutor.builder()
            .cliPath("/nonexistent/path/to/cli")
            .build();

        assertThat(executor.isCliAvailable()).isFalse();
    }
}
