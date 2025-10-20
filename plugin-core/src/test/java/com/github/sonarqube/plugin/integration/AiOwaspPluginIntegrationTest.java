package com.github.sonarqube.plugin.integration;

import com.github.sonarqube.plugin.AiOwaspPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI OWASP Plugin 整合測試
 *
 * 測試範圍：
 * - Plugin 載入與初始化
 * - Extension 註冊（配置、Web Service、Web Page）
 * - SonarQube 版本相容性
 *
 * @since 3.0.0 (Epic 8, Story 8.2)
 */
@DisplayName("AiOwaspPlugin Integration Tests")
public class AiOwaspPluginIntegrationTest {

    @Test
    @DisplayName("測試 Plugin 正確載入")
    void testPluginLoads() {
        AiOwaspPlugin plugin = new AiOwaspPlugin();
        assertNotNull(plugin, "Plugin 應該能夠正常建立");
    }

    @Test
    @DisplayName("測試 Plugin define() 方法執行")
    void testPluginDefineMethod() {
        AiOwaspPlugin plugin = new AiOwaspPlugin();

        // 建立測試用的 Plugin Context
        Plugin.Context context = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.COMMUNITY
                )
        );

        // 執行 define() 不應該拋出異常
        assertDoesNotThrow(() -> plugin.define(context));
    }

    @Test
    @DisplayName("測試註冊的 Extension 數量")
    void testExtensionCount() {
        AiOwaspPlugin plugin = new AiOwaspPlugin();

        Plugin.Context context = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.COMMUNITY
                )
        );

        plugin.define(context);

        // 預期註冊的 Extension 數量
        // 17 個配置屬性 + 5 個 Web Service + 1 個 Web Page = 23 個
        assertTrue(context.getExtensions().size() >= 23,
                "應該註冊至少 23 個 Extension，實際: " + context.getExtensions().size());
    }

    @Test
    @DisplayName("測試配置屬性註冊")
    void testConfigurationPropertiesRegistered() {
        AiOwaspPlugin plugin = new AiOwaspPlugin();

        Plugin.Context context = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.COMMUNITY
                )
        );

        plugin.define(context);

        // 檢查是否包含 PropertyDefinition
        long propertyDefinitionCount = context.getExtensions().stream()
                .filter(ext -> ext instanceof org.sonar.api.config.PropertyDefinition)
                .count();

        // 應該有 17 個配置屬性
        assertEquals(17, propertyDefinitionCount,
                "應該註冊 17 個配置屬性");
    }

    @Test
    @DisplayName("測試 Web Service 註冊")
    void testWebServicesRegistered() {
        AiOwaspPlugin plugin = new AiOwaspPlugin();

        Plugin.Context context = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.COMMUNITY
                )
        );

        plugin.define(context);

        // 檢查是否包含 WebService
        long webServiceCount = context.getExtensions().stream()
                .filter(ext -> ext instanceof org.sonar.api.server.ws.WebService)
                .count();

        // 應該有 5 個 Web Service
        assertEquals(5, webServiceCount,
                "應該註冊 5 個 Web Service");
    }

    @Test
    @DisplayName("測試 Web Page 註冊")
    void testWebPagesRegistered() {
        AiOwaspPlugin plugin = new AiOwaspPlugin();

        Plugin.Context context = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.COMMUNITY
                )
        );

        plugin.define(context);

        // 檢查是否包含 PageDefinition
        long pageDefinitionCount = context.getExtensions().stream()
                .filter(ext -> ext.toString().contains("PageDefinition"))
                .count();

        // 應該有至少 1 個 Web Page
        assertTrue(pageDefinitionCount >= 1,
                "應該註冊至少 1 個 Web Page");
    }

    @Test
    @DisplayName("測試 SonarQube 9.9 相容性")
    void testSonarQube99Compatibility() {
        AiOwaspPlugin plugin = new AiOwaspPlugin();

        Plugin.Context context = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.COMMUNITY
                )
        );

        // 應該能夠在 SonarQube 9.9 上正常執行
        assertDoesNotThrow(() -> plugin.define(context));
    }

    @Test
    @DisplayName("測試 SonarQube 10.x 相容性")
    void testSonarQube10Compatibility() {
        AiOwaspPlugin plugin = new AiOwaspPlugin();

        Plugin.Context context = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(10, 0),
                        SonarQubeSide.SERVER,
                        SonarEdition.COMMUNITY
                )
        );

        // 應該能夠在 SonarQube 10.x 上正常執行
        assertDoesNotThrow(() -> plugin.define(context));
    }

    @Test
    @DisplayName("測試不同 SonarQube Edition 相容性")
    void testDifferentEditionsCompatibility() {
        AiOwaspPlugin plugin = new AiOwaspPlugin();

        // Community Edition
        Plugin.Context communityContext = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.COMMUNITY
                )
        );
        assertDoesNotThrow(() -> plugin.define(communityContext));

        // Developer Edition
        Plugin.Context developerContext = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.DEVELOPER
                )
        );
        assertDoesNotThrow(() -> plugin.define(developerContext));

        // Enterprise Edition
        Plugin.Context enterpriseContext = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.ENTERPRISE
                )
        );
        assertDoesNotThrow(() -> plugin.define(enterpriseContext));
    }

    @Test
    @DisplayName("測試 Plugin 多次初始化")
    void testMultipleInitializations() {
        AiOwaspPlugin plugin = new AiOwaspPlugin();

        Plugin.Context context1 = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.COMMUNITY
                )
        );

        Plugin.Context context2 = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.COMMUNITY
                )
        );

        // 應該能夠多次初始化（例如測試環境）
        assertDoesNotThrow(() -> {
            plugin.define(context1);
            plugin.define(context2);
        });
    }

    @Test
    @DisplayName("測試特定 Extension 類型存在")
    void testSpecificExtensionTypesExist() {
        AiOwaspPlugin plugin = new AiOwaspPlugin();

        Plugin.Context context = new Plugin.Context(
                SonarRuntimeImpl.forSonarQube(
                        Version.create(9, 9),
                        SonarQubeSide.SERVER,
                        SonarEdition.COMMUNITY
                )
        );

        plugin.define(context);

        // 檢查特定類型的 Extension
        boolean hasConfigurationApi = context.getExtensions().stream()
                .anyMatch(ext -> ext.getClass().getSimpleName().equals("ConfigurationApiController"));

        boolean hasScanProgressApi = context.getExtensions().stream()
                .anyMatch(ext -> ext.getClass().getSimpleName().equals("ScanProgressApiController"));

        boolean hasCliStatusApi = context.getExtensions().stream()
                .anyMatch(ext -> ext.getClass().getSimpleName().equals("CliStatusApiController"));

        assertTrue(hasConfigurationApi, "應該註冊 ConfigurationApiController");
        assertTrue(hasScanProgressApi, "應該註冊 ScanProgressApiController");
        assertTrue(hasCliStatusApi, "應該註冊 CliStatusApiController");
    }

    @Test
    @DisplayName("測試 Plugin 資訊正確性")
    void testPluginMetadata() {
        // 驗證 Plugin 類別資訊
        assertEquals("AiOwaspPlugin", AiOwaspPlugin.class.getSimpleName());
        assertEquals("com.github.sonarqube.plugin", AiOwaspPlugin.class.getPackage().getName());
    }

    @Test
    @DisplayName("測試 Plugin 常數定義")
    void testPluginConstants() {
        // 驗證 Plugin 定義的常數
        assertEquals("AI Configuration", AiOwaspPlugin.CATEGORY_AI);
        assertEquals("OWASP Versions", AiOwaspPlugin.CATEGORY_OWASP);
        assertEquals("Performance", AiOwaspPlugin.CATEGORY_PERFORMANCE);
        assertEquals("Reporting", AiOwaspPlugin.CATEGORY_REPORT);

        // 驗證配置屬性 key
        assertEquals("sonar.aiowasp.ai.provider", AiOwaspPlugin.PROPERTY_AI_PROVIDER);
        assertEquals("sonar.aiowasp.ai.apikey", AiOwaspPlugin.PROPERTY_AI_API_KEY);
        assertEquals("sonar.aiowasp.cache.enabled", AiOwaspPlugin.PROPERTY_CACHE_ENABLED);
    }
}
