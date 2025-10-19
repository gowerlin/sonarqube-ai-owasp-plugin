package com.github.sonarqube.plugin;

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiOwaspPlugin 單元測試
 *
 * @since 1.0.0
 */
class AiOwaspPluginTest {

    @Test
    void testPluginDefinition() {
        // Given
        AiOwaspPlugin plugin = new AiOwaspPlugin();
        SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(
            Version.create(9, 9),
            org.sonar.api.SonarProduct.SONARQUBE,
            org.sonar.api.SonarQubeSide.SCANNER
        );
        Plugin.Context context = new Plugin.Context(runtime);

        // When
        plugin.define(context);

        // Then
        assertThat(context.getExtensions())
            .as("Plugin should register extensions")
            .isNotEmpty();

        assertThat(context.getExtensions().size())
            .as("Plugin should register 14 configuration properties")
            .isEqualTo(14);
    }

    @Test
    void testPluginCanBeInstantiated() {
        // When & Then
        assertThat(new AiOwaspPlugin())
            .as("Plugin should be instantiable")
            .isNotNull()
            .isInstanceOf(Plugin.class);
    }
}
