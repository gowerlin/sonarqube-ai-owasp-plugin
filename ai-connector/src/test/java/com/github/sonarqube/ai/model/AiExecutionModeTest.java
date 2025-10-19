package com.github.sonarqube.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for AiExecutionMode
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9)
 */
class AiExecutionModeTest {

    @Test
    void testEnumValues() {
        assertThat(AiExecutionMode.values()).hasSize(2);
        assertThat(AiExecutionMode.valueOf("API")).isEqualTo(AiExecutionMode.API);
        assertThat(AiExecutionMode.valueOf("CLI")).isEqualTo(AiExecutionMode.CLI);
    }

    @Test
    void testGetCode() {
        assertThat(AiExecutionMode.API.getCode()).isEqualTo("api");
        assertThat(AiExecutionMode.CLI.getCode()).isEqualTo("cli");
    }

    @Test
    void testGetDisplayName() {
        assertThat(AiExecutionMode.API.getDisplayName()).isEqualTo("API 模式");
        assertThat(AiExecutionMode.CLI.getDisplayName()).isEqualTo("CLI 模式");
    }

    @Test
    void testGetDescription() {
        assertThat(AiExecutionMode.API.getDescription()).contains("HTTP REST API");
        assertThat(AiExecutionMode.CLI.getDescription()).contains("命令列工具");
    }

    @Test
    void testFromCodeWithValidCode() {
        assertThat(AiExecutionMode.fromCode("api")).isEqualTo(AiExecutionMode.API);
        assertThat(AiExecutionMode.fromCode("cli")).isEqualTo(AiExecutionMode.CLI);
    }

    @Test
    void testFromCodeIsCaseInsensitive() {
        assertThat(AiExecutionMode.fromCode("API")).isEqualTo(AiExecutionMode.API);
        assertThat(AiExecutionMode.fromCode("CLI")).isEqualTo(AiExecutionMode.CLI);
        assertThat(AiExecutionMode.fromCode("Api")).isEqualTo(AiExecutionMode.API);
        assertThat(AiExecutionMode.fromCode("Cli")).isEqualTo(AiExecutionMode.CLI);
    }

    @Test
    void testFromCodeHandlesWhitespace() {
        assertThat(AiExecutionMode.fromCode("  api  ")).isEqualTo(AiExecutionMode.API);
        assertThat(AiExecutionMode.fromCode("  cli  ")).isEqualTo(AiExecutionMode.CLI);
    }

    @Test
    void testFromCodeWithNullReturnsDefault() {
        assertThat(AiExecutionMode.fromCode(null)).isEqualTo(AiExecutionMode.API);
    }

    @Test
    void testFromCodeWithEmptyStringReturnsDefault() {
        assertThat(AiExecutionMode.fromCode("")).isEqualTo(AiExecutionMode.API);
        assertThat(AiExecutionMode.fromCode("   ")).isEqualTo(AiExecutionMode.API);
    }

    @Test
    void testFromCodeWithInvalidCodeThrowsException() {
        assertThatThrownBy(() -> AiExecutionMode.fromCode("invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown execution mode: invalid");
    }

    @Test
    void testIsApi() {
        assertThat(AiExecutionMode.API.isApi()).isTrue();
        assertThat(AiExecutionMode.CLI.isApi()).isFalse();
    }

    @Test
    void testIsCli() {
        assertThat(AiExecutionMode.API.isCli()).isFalse();
        assertThat(AiExecutionMode.CLI.isCli()).isTrue();
    }

    @Test
    void testToString() {
        assertThat(AiExecutionMode.API.toString()).isEqualTo("API 模式 (api)");
        assertThat(AiExecutionMode.CLI.toString()).isEqualTo("CLI 模式 (cli)");
    }
}
