package com.github.sonarqube.plugin.api;

import com.github.sonarqube.rules.OwaspVersionManager;
import com.github.sonarqube.rules.OwaspVersionMappingService;
import com.github.sonarqube.rules.RuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ws.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OWASP Version Management API Controller
 *
 * <p>提供 OWASP 版本管理 API，支援版本查詢、切換和映射查詢。</p>
 *
 * <p>API 端點：</p>
 * <ul>
 *   <li>{@code /api/owasp/version/list} - 取得支援的 OWASP 版本列表</li>
 *   <li>{@code /api/owasp/version/current} - 取得當前活躍版本</li>
 *   <li>{@code /api/owasp/version/switch?version=<version>} - 切換 OWASP 版本</li>
 *   <li>{@code /api/owasp/version/mappings} - 取得版本映射關係</li>
 * </ul>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.2.0 (Epic 4, Story 4.4)
 */
public class OwaspVersionApiController implements WebService {

    private static final Logger LOG = LoggerFactory.getLogger(OwaspVersionApiController.class);

    private static final String API_ENDPOINT = "api/owasp";
    private static final String CONTROLLER_NAME = "version";

    private final OwaspVersionManager versionManager;
    private final OwaspVersionMappingService mappingService;

    /**
     * 建構子
     *
     * @param ruleRegistry 規則註冊表
     */
    public OwaspVersionApiController(RuleRegistry ruleRegistry) {
        this.versionManager = new OwaspVersionManager(ruleRegistry);
        this.mappingService = new OwaspVersionMappingService();
    }

    @Override
    public void define(Context context) {
        NewController controller = context.createController(API_ENDPOINT);

        // GET /api/owasp/version/list
        defineListAction(controller);

        // GET /api/owasp/version/current
        defineCurrentAction(controller);

        // POST /api/owasp/version/switch
        defineSwitchAction(controller);

        // GET /api/owasp/version/mappings
        defineMappingsAction(controller);

        controller.done();
    }

    /**
     * 定義版本列表 API
     */
    private void defineListAction(NewController controller) {
        controller.createAction("version/list")
            .setDescription("Get supported OWASP versions")
            .setSince("2.2.0")
            .setHandler(this::handleListAction);
    }

    /**
     * 定義當前版本 API
     */
    private void defineCurrentAction(NewController controller) {
        controller.createAction("version/current")
            .setDescription("Get current active OWASP version")
            .setSince("2.2.0")
            .setHandler(this::handleCurrentAction);
    }

    /**
     * 定義版本切換 API
     */
    private void defineSwitchAction(NewController controller) {
        NewAction switchAction = controller.createAction("version/switch")
            .setDescription("Switch OWASP version")
            .setSince("2.2.0")
            .setPost(true)
            .setHandler(this::handleSwitchAction);

        switchAction.createParam("version")
                .setRequired(true)
                .setDescription("Target OWASP version (2017 or 2021)")
                .setExampleValue("2021");
    }

    /**
     * 定義版本映射 API
     */
    private void defineMappingsAction(NewController controller) {
        NewAction mappingsAction = controller.createAction("version/mappings")
            .setDescription("Get OWASP version mappings (2017 ↔ 2021)")
            .setSince("2.2.0")
            .setHandler(this::handleMappingsAction);

        mappingsAction.createParam("sourceVersion")
                .setRequired(false)
                .setDescription("Source version to filter mappings")
                .setExampleValue("2017");
        mappingsAction.createParam("sourceCategory")
                .setRequired(false)
                .setDescription("Source category to filter mappings")
                .setExampleValue("A1");
    }

    /**
     * 處理版本列表請求
     */
    private void handleListAction(Request request, Response response) {
        LOG.info("Handling version list request");

        try {
            List<OwaspVersionManager.OwaspVersion> versions = versionManager.getSupportedVersions();

            String json = buildVersionListJson(versions);

            response.stream().setMediaType("application/json");
            response.stream().output().write(json.getBytes());

        } catch (IOException e) {
            LOG.error("Error handling version list request", e);
            response.stream().setStatus(500);
        }
    }

    /**
     * 處理當前版本請求
     */
    private void handleCurrentAction(Request request, Response response) {
        LOG.info("Handling current version request");

        try {
            OwaspVersionManager.OwaspVersion currentVersion = versionManager.getActiveVersion();
            int ruleCount = versionManager.getRuleCountForVersion(currentVersion);
            List<String> categories = versionManager.getCategoriesForVersion(currentVersion);

            String json = buildCurrentVersionJson(currentVersion, ruleCount, categories);

            response.stream().setMediaType("application/json");
            response.stream().output().write(json.getBytes());

        } catch (IOException e) {
            LOG.error("Error handling current version request", e);
            response.stream().setStatus(500);
        }
    }

    /**
     * 處理版本切換請求
     */
    private void handleSwitchAction(Request request, Response response) {
        String targetVersionStr = request.mandatoryParam("version");
        LOG.info("Handling version switch request: {}", targetVersionStr);

        try {
            if (!versionManager.isVersionSupported(targetVersionStr)) {
                response.stream().setStatus(400);
                response.stream().output().write("{\"error\":\"Unsupported version\"}".getBytes());
                return;
            }

            OwaspVersionManager.OwaspVersion currentVersion = versionManager.getActiveVersion();
            OwaspVersionManager.OwaspVersion targetVersion = OwaspVersionManager.OwaspVersion.fromString(targetVersionStr);

            OwaspVersionManager.VersionSwitchInfo switchInfo = versionManager.switchVersion(currentVersion, targetVersion);

            String json = buildSwitchInfoJson(switchInfo);

            response.stream().setMediaType("application/json");
            response.stream().output().write(json.getBytes());

        } catch (IOException e) {
            LOG.error("Error handling version switch request", e);
            response.stream().setStatus(500);
        }
    }

    /**
     * 處理版本映射請求
     */
    private void handleMappingsAction(Request request, Response response) {
        String sourceVersion = request.param("sourceVersion");
        String sourceCategory = request.param("sourceCategory");

        LOG.info("Handling mappings request: sourceVersion={}, sourceCategory={}", sourceVersion, sourceCategory);

        try {
            List<OwaspVersionMappingService.CategoryMapping> mappings;

            if (sourceVersion != null && sourceCategory != null) {
                mappings = mappingService.getMappings(sourceVersion, sourceCategory);
            } else if ("2017".equals(sourceVersion)) {
                mappings = mappingService.get2017To2021Mappings();
            } else {
                mappings = mappingService.getAllMappings();
            }

            String json = buildMappingsJson(mappings);

            response.stream().setMediaType("application/json");
            response.stream().output().write(json.getBytes());

        } catch (IOException e) {
            LOG.error("Error handling mappings request", e);
            response.stream().setStatus(500);
        }
    }

    // JSON Builder Methods

    private String buildVersionListJson(List<OwaspVersionManager.OwaspVersion> versions) {
        StringBuilder json = new StringBuilder("{\"versions\":[");

        for (int i = 0; i < versions.size(); i++) {
            OwaspVersionManager.OwaspVersion version = versions.get(i);
            int ruleCount = versionManager.getRuleCountForVersion(version);

            if (i > 0) json.append(",");
            json.append("{")
                .append("\"version\":\"").append(version.getVersion()).append("\",")
                .append("\"displayName\":\"").append(version.getDisplayName()).append("\",")
                .append("\"ruleCount\":").append(ruleCount)
                .append("}");
        }

        json.append("]}");
        return json.toString();
    }

    private String buildCurrentVersionJson(OwaspVersionManager.OwaspVersion version, int ruleCount, List<String> categories) {
        StringBuilder json = new StringBuilder("{");

        json.append("\"version\":\"").append(version.getVersion()).append("\",")
            .append("\"displayName\":\"").append(version.getDisplayName()).append("\",")
            .append("\"ruleCount\":").append(ruleCount).append(",")
            .append("\"categories\":[");

        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(categories.get(i)).append("\"");
        }

        json.append("]}");
        return json.toString();
    }

    private String buildSwitchInfoJson(OwaspVersionManager.VersionSwitchInfo switchInfo) {
        StringBuilder json = new StringBuilder("{");

        json.append("\"fromVersion\":\"").append(switchInfo.getFromVersion()).append("\",")
            .append("\"toVersion\":\"").append(switchInfo.getToVersion()).append("\",")
            .append("\"fromRuleCount\":").append(switchInfo.getFromRuleCount()).append(",")
            .append("\"toRuleCount\":").append(switchInfo.getToRuleCount()).append(",")
            .append("\"availableCategories\":[");

        List<String> categories = switchInfo.getAvailableCategories();
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(categories.get(i)).append("\"");
        }

        json.append("]}");
        return json.toString();
    }

    private String buildMappingsJson(List<OwaspVersionMappingService.CategoryMapping> mappings) {
        StringBuilder json = new StringBuilder("{\"mappings\":[");

        for (int i = 0; i < mappings.size(); i++) {
            OwaspVersionMappingService.CategoryMapping mapping = mappings.get(i);

            if (i > 0) json.append(",");
            json.append("{")
                .append("\"sourceVersion\":\"").append(mapping.getSourceVersion()).append("\",")
                .append("\"sourceCategory\":\"").append(mapping.getSourceCategory()).append("\",")
                .append("\"sourceName\":\"").append(escapeJson(mapping.getSourceName())).append("\",");

            if (mapping.getTargetVersion() != null) {
                json.append("\"targetVersion\":\"").append(mapping.getTargetVersion()).append("\",")
                    .append("\"targetCategory\":\"").append(mapping.getTargetCategory()).append("\",")
                    .append("\"targetName\":\"").append(escapeJson(mapping.getTargetName())).append("\",");
            }

            json.append("\"mappingType\":\"").append(mapping.getMappingType().name()).append("\",")
                .append("\"explanation\":\"").append(escapeJson(mapping.getExplanation())).append("\"")
                .append("}");
        }

        json.append("]}");
        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"")
                  .replace("\\", "\\\\")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
