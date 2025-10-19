.PHONY: help build test clean package install start stop logs restart

# 預設目標
help:
	@echo "SonarQube AI OWASP Plugin - 開發指令"
	@echo ""
	@echo "建構相關："
	@echo "  make build       - 編譯專案"
	@echo "  make test        - 執行測試"
	@echo "  make package     - 打包插件 JAR"
	@echo "  make clean       - 清理建構產物"
	@echo ""
	@echo "Docker 環境："
	@echo "  make start       - 啟動 SonarQube 開發環境"
	@echo "  make stop        - 停止 SonarQube 開發環境"
	@echo "  make restart     - 重啟 SonarQube"
	@echo "  make logs        - 查看 SonarQube 日誌"
	@echo ""
	@echo "開發流程："
	@echo "  make install     - 編譯並複製插件至 SonarQube"
	@echo "  make dev         - 完整開發循環（build + install + restart）"

# 使用 Docker 建構環境執行 Maven
DOCKER_BUILD=docker run --rm -v $(PWD):/workspace -w /workspace maven:3.9-eclipse-temurin-11 mvn

# 編譯專案
build:
	@echo "📦 編譯專案..."
	$(DOCKER_BUILD) clean compile

# 執行測試
test:
	@echo "🧪 執行測試..."
	$(DOCKER_BUILD) test

# 打包插件
package:
	@echo "📦 打包插件..."
	$(DOCKER_BUILD) clean package -DskipTests
	@echo "✅ 插件 JAR 檔案位於: plugin-core/target/sonar-aiowasp-plugin-*.jar"

# 清理建構產物
clean:
	@echo "🧹 清理建構產物..."
	$(DOCKER_BUILD) clean

# 安裝插件至本地 SonarQube
install: package
	@echo "📥 複製插件至 SonarQube..."
	@mkdir -p ./sonarqube_plugins
	@cp plugin-core/target/sonar-aiowasp-plugin-*.jar ./sonarqube_plugins/
	@echo "✅ 插件已複製，請重啟 SonarQube 以載入"

# 啟動 SonarQube 開發環境
start:
	@echo "🚀 啟動 SonarQube 開發環境..."
	docker-compose up -d
	@echo "⏳ 等待 SonarQube 啟動（約 60 秒）..."
	@sleep 10
	@echo "✅ SonarQube 正在啟動，請訪問 http://localhost:9000"
	@echo "   預設帳號：admin / admin"

# 停止 SonarQube
stop:
	@echo "🛑 停止 SonarQube..."
	docker-compose down

# 重啟 SonarQube
restart:
	@echo "🔄 重啟 SonarQube..."
	docker-compose restart sonarqube
	@echo "⏳ 等待 SonarQube 重啟..."
	@sleep 15
	@echo "✅ SonarQube 已重啟"

# 查看 SonarQube 日誌
logs:
	docker-compose logs -f sonarqube

# 完整開發循環
dev: build install restart
	@echo "✅ 開發循環完成！"
	@echo "   1. 程式碼已編譯"
	@echo "   2. 插件已安裝"
	@echo "   3. SonarQube 已重啟"
	@echo "   請訪問 http://localhost:9000 驗證插件"

# 驗證環境
check-env:
	@echo "🔍 檢查開發環境..."
	@docker --version || (echo "❌ Docker 未安裝"; exit 1)
	@docker-compose --version || (echo "❌ Docker Compose 未安裝"; exit 1)
	@echo "✅ 環境檢查完成"
