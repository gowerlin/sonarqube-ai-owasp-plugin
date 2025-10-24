// SonarQube Page Extension: OWASP Security Report
// 直接載入 HTML 內容而非使用 iframe

// ==================== 全域變數 ====================
let allFindings = [];
let filteredFindings = [];
let currentOwaspVersion = '2021';
let projectKey = 'unknown';

// OWASP 版本定義
const OWASP_VERSIONS = {
    '2017': {
        'A01': 'A1: 注入攻擊',
        'A02': 'A2: 無效的身份驗證',
        'A03': 'A3: 敏感數據洩露',
        'A04': 'A4: XML 外部實體 (XXE)',
        'A05': 'A5: 無效的存取控制',
        'A06': 'A6: 安全設定錯誤',
        'A07': 'A7: 跨站腳本 (XSS)',
        'A08': 'A8: 不安全的反序列化',
        'A09': 'A9: 使用已知漏洞的元件',
        'A10': 'A10: 記錄與監控不足'
    },
    '2021': {
        'A01': 'A01: 存取控制失效',
        'A02': 'A02: 加密機制失效',
        'A03': 'A03: 注入攻擊',
        'A04': 'A04: 不安全設計',
        'A05': 'A05: 安全設定缺陷',
        'A06': 'A06: 危險或過時的元件',
        'A07': 'A07: 身份識別及驗證失效',
        'A08': 'A08: 軟體及資料完整性失效',
        'A09': 'A09: 安全記錄及監控失效',
        'A10': 'A10: 伺服器端請求偽造 (SSRF)'
    },
    '2025': {
        'A01': 'A01: 存取控制失效',
        'A02': 'A02: 加密機制失效',
        'A03': 'A03: 注入攻擊',
        'A04': 'A04: 不安全設計',
        'A05': 'A05: 安全設定缺陷',
        'A06': 'A06: 危險或過時的元件',
        'A07': 'A07: 身份識別及驗證失效',
        'A08': 'A08: 軟體及資料完整性失效',
        'A09': 'A09: 安全記錄及監控失效',
        'A10': 'A10: 不安全的 AI 使用'
    }
};

// 嚴重性標籤映射
const severityLabels = {
    'BLOCKER': '阻斷',
    'CRITICAL': '嚴重',
    'MAJOR': '中',
    'MINOR': '低',
    'INFO': '資訊'
};

// ==================== 工具函數 ====================

// Escape HTML to prevent XSS
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ==================== OWASP 版本管理 ====================

// 更新 OWASP 分類選項
function updateOwaspCategoryOptions() {
    const owaspFilter = document.getElementById('owaspFilter');
    if (!owaspFilter) return;

    const categories = OWASP_VERSIONS[currentOwaspVersion];
    const currentValue = owaspFilter.value;
    owaspFilter.innerHTML = '<option value="">所有分類</option>';

    Object.entries(categories).forEach(([key, label]) => {
        const option = document.createElement('option');
        option.value = key;
        option.textContent = label;
        owaspFilter.appendChild(option);
    });

    if (currentValue && categories[currentValue]) {
        owaspFilter.value = currentValue;
    }

    console.log('[OWASP Report] Updated category options for version ' + currentOwaspVersion);
}

// ==================== 下載報告 ====================

// Download report in specified format
function downloadReport(format) {
    const validFormats = ['pdf', 'html', 'json', 'markdown'];
    if (!validFormats.includes(format)) {
        console.error('無效的格式:', format);
        return;
    }

    const downloadUrl = `/api/owasp/report/export?format=${format}&project=${encodeURIComponent(projectKey)}&version=${currentOwaspVersion}`;
    console.log(`[OWASP Report] Downloading ${format.toUpperCase()} report for project: ${projectKey}`);
    console.log(`[OWASP Report] Download URL: ${downloadUrl}`);

    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = `owasp-security-report-${projectKey.replace(/:/g, '-')}.${format === 'markdown' ? 'md' : format}`;
    link.style.display = 'none';

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    console.log(`[OWASP Report] Download triggered for ${format.toUpperCase()} format`);
}

// ==================== 資料載入 ====================

// Initialize page
async function initReport() {
    showLoading();
    await loadReportData();
    populateFileFilter();
    applyFilters();
}

// Load report data from API
async function loadReportData() {
    try {
        const response = await fetch(`/api/owasp/report/export?project=${projectKey}&format=json&version=${currentOwaspVersion}`);
        const data = await response.json();
        allFindings = parseFindings(data);
        console.log('[OWASP Report] Loaded findings:', allFindings.length);
    } catch (error) {
        console.error('無法載入報告資料:', error);
        allFindings = generateMockData();
    }
}

// Parse findings from JSON report structure
function parseFindings(data) {
    const findings = [];

    if (data.versions && Array.isArray(data.versions)) {
        data.versions.forEach(version => {
            if (version.findings) {
                version.findings.forEach(finding => {
                    findings.push({
                        ...finding,
                        owaspVersion: version.owaspVersion
                    });
                });
            }
        });
    } else if (data.findings && Array.isArray(data.findings)) {
        findings.push(...data.findings);
    }

    return findings;
}

// ==================== 篩選功能 ====================

// Populate file filter dropdown
function populateFileFilter() {
    const fileFilter = document.getElementById('fileFilter');
    if (!fileFilter) return;

    const uniqueFiles = [...new Set(allFindings.map(f => f.filePath))].sort();
    fileFilter.innerHTML = '<option value="">所有檔案</option>';

    uniqueFiles.forEach(file => {
        const option = document.createElement('option');
        option.value = file;
        option.textContent = file;
        fileFilter.appendChild(option);
    });
}

// Apply filters
function applyFilters() {
    const severityFilter = document.getElementById('severityFilter')?.value || '';
    const owaspFilter = document.getElementById('owaspFilter')?.value || '';
    const fileFilter = document.getElementById('fileFilter')?.value || '';
    const searchText = document.getElementById('searchBox')?.value.toLowerCase() || '';

    filteredFindings = allFindings.filter(finding => {
        if (severityFilter && finding.severity !== severityFilter) return false;
        if (owaspFilter && !finding.owaspCategory?.startsWith(owaspFilter)) return false;
        if (fileFilter && finding.filePath !== fileFilter) return false;

        if (searchText) {
            const searchableText = [
                finding.title,
                finding.description,
                finding.cweId,
                finding.owaspCategory
            ].join(' ').toLowerCase();

            if (!searchableText.includes(searchText)) return false;
        }

        return true;
    });

    updateSummaryStats();
    renderFindings();
}

// Reset filters
function resetFilters() {
    if (document.getElementById('severityFilter')) document.getElementById('severityFilter').value = '';
    if (document.getElementById('owaspFilter')) document.getElementById('owaspFilter').value = '';
    if (document.getElementById('fileFilter')) document.getElementById('fileFilter').value = '';
    if (document.getElementById('searchBox')) document.getElementById('searchBox').value = '';
    applyFilters();
}

// ==================== 統計資料更新 ====================

// Update summary statistics
function updateSummaryStats() {
    const severityMap = {
        'BLOCKER': 'CRITICAL',
        'CRITICAL': 'HIGH',
        'MAJOR': 'MEDIUM',
        'MINOR': 'LOW',
        'INFO': 'INFO'
    };

    const stats = {
        CRITICAL: 0,
        HIGH: 0,
        MEDIUM: 0,
        LOW: 0,
        INFO: 0
    };

    filteredFindings.forEach(finding => {
        const mappedSeverity = severityMap[finding.severity] || finding.severity;
        if (stats.hasOwnProperty(mappedSeverity)) {
            stats[mappedSeverity]++;
        }
    });

    const criticalCount = document.getElementById('criticalCount');
    const highCount = document.getElementById('highCount');
    const mediumCount = document.getElementById('mediumCount');
    const lowCount = document.getElementById('lowCount');
    const infoCount = document.getElementById('infoCount');
    const findingsCount = document.getElementById('findingsCount');

    if (criticalCount) criticalCount.textContent = stats.CRITICAL;
    if (highCount) highCount.textContent = stats.HIGH;
    if (mediumCount) mediumCount.textContent = stats.MEDIUM;
    if (lowCount) lowCount.textContent = stats.LOW;
    if (infoCount) infoCount.textContent = stats.INFO;
    if (findingsCount) findingsCount.textContent = `${filteredFindings.length} 個發現`;
}

// ==================== 渲染發現列表 ====================

// Render findings list
function renderFindings() {
    const findingsList = document.getElementById('findingsList');
    if (!findingsList) return;

    if (filteredFindings.length === 0) {
        findingsList.innerHTML = '<div class="empty-state">沒有符合篩選條件的發現</div>';
        return;
    }

    findingsList.innerHTML = filteredFindings.map((finding, index) => `
        <div class="finding-card" data-index="${index}">
            <div class="finding-header">
                <span class="severity-badge severity-${finding.severity.toLowerCase()}">${severityLabels[finding.severity] || finding.severity}</span>
                <div class="finding-title">
                    <h3>${escapeHtml(finding.ruleName || finding.title)}</h3>
                    <div class="finding-meta">
                        <span>📂 ${escapeHtml(finding.filePath)}</span>
                        <span>📍 第 ${finding.lineNumber || 'N/A'} 行</span>
                        <span>🔖 ${escapeHtml(finding.owaspCategory || 'N/A')}</span>
                        <span>🔗 ${escapeHtml((finding.cweIds && finding.cweIds.length > 0 ? finding.cweIds[0] : finding.cweId) || 'N/A')}</span>
                    </div>
                </div>
            </div>
            <div class="finding-description">
                ${escapeHtml(finding.description)}
            </div>
            <div class="finding-location">
                <strong>位置：</strong> ${escapeHtml(finding.filePath)}:${finding.lineNumber || '?'}
            </div>
            <div class="finding-tags">
                ${finding.tags ? finding.tags.map(tag => `<span class="tag">${escapeHtml(tag)}</span>`).join('') : ''}
            </div>
            <div class="finding-details">
                ${finding.codeSnippet ? `
                    <div class="code-snippet">
                        <pre>${escapeHtml(finding.codeSnippet)}</pre>
                    </div>
                ` : ''}
                ${finding.recommendation ? `
                    <div class="recommendation">
                        <h4>💡 建議</h4>
                        <p>${escapeHtml(finding.recommendation)}</p>
                    </div>
                ` : ''}
                ${finding.fixSuggestion ? `
                    <div class="recommendation">
                        <h4>🔧 修復建議</h4>
                        <p>${escapeHtml(finding.fixSuggestion)}</p>
                    </div>
                ` : ''}
            </div>
        </div>
    `).join('');

    // Add event listeners to finding cards
    const cards = document.querySelectorAll('.finding-card');
    cards.forEach(card => {
        card.addEventListener('click', function() {
            this.classList.toggle('expanded');
        });
    });
}

// ==================== UI 狀態 ====================

// Show loading state
function showLoading() {
    const findingsList = document.getElementById('findingsList');
    if (findingsList) {
        findingsList.innerHTML = '<div class="loading">正在載入報告資料...</div>';
    }
}

// ==================== Mock 資料 ====================

// Generate mock data for demonstration
function generateMockData() {
    return [
        {
            severity: 'MAJOR',
            title: 'SQL Injection Vulnerability',
            description: 'User input is directly concatenated into SQL query without proper sanitization.',
            filePath: 'src/main/java/com/example/UserService.java',
            lineNumber: 45,
            owaspCategory: 'A03:2021-Injection',
            cweId: 'CWE-89',
            tags: ['injection', 'database', 'security'],
            codeSnippet: 'String query = "SELECT * FROM users WHERE username = \'" + username + "\'";',
            recommendation: 'Use parameterized queries (PreparedStatement) instead of string concatenation.',
            fixSuggestion: 'PreparedStatement ps = connection.prepareStatement("SELECT * FROM users WHERE username = ?");\nps.setString(1, username);'
        },
        {
            severity: 'MINOR',
            title: 'Hardcoded Credentials',
            description: 'Database password is hardcoded in source code.',
            filePath: 'src/main/resources/application.properties',
            lineNumber: 12,
            owaspCategory: 'A02:2021-Cryptographic Failures',
            cweId: 'CWE-798',
            tags: ['credentials', 'configuration'],
            codeSnippet: 'spring.datasource.password=admin123',
            recommendation: 'Store credentials in environment variables or secure vault.',
            fixSuggestion: 'Use ${DB_PASSWORD} and set environment variable.'
        }
    ];
}

// ==================== 事件監聽器設置 ====================

// Setup event listeners
function setupEventListeners(container) {
    // Download buttons
    const downloadBtns = container.querySelectorAll('.btn-download');
    console.log('[OWASP Report] Found', downloadBtns.length, 'download buttons');
    downloadBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const format = this.getAttribute('data-format');
            console.log('[OWASP Report] Download button clicked:', format);
            downloadReport(format);
        });
    });

    // OWASP Version selector
    const versionSelect = container.querySelector('#owaspVersionSelect');
    if (versionSelect) {
        versionSelect.addEventListener('change', function() {
            currentOwaspVersion = this.value;
            console.log('[OWASP Report] Version changed to:', currentOwaspVersion);
            updateOwaspCategoryOptions();
            loadReportData().then(() => {
                populateFileFilter();
                applyFilters();
            });
        });
    }

    // Filter buttons
    const applyBtn = container.querySelector('#applyFiltersBtn');
    const resetBtn = container.querySelector('#resetFiltersBtn');

    if (applyBtn) {
        applyBtn.addEventListener('click', applyFilters);
        console.log('[OWASP Report] Apply filter button listener attached');
    }

    if (resetBtn) {
        resetBtn.addEventListener('click', resetFilters);
        console.log('[OWASP Report] Reset filter button listener attached');
    }

    // Search box - trigger filter on Enter key
    const searchBox = container.querySelector('#searchBox');
    if (searchBox) {
        searchBox.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                applyFilters();
            }
        });
        console.log('[OWASP Report] Search box Enter key listener attached');
    }
}

// ==================== SonarQube Extension 註冊 ====================

window.registerExtension('aiowasp/report', function (options) {
    console.log('[OWASP Report] Extension starting...', options);

    // 獲取專案 key 和其他參數
    projectKey = options.component.key;
    const branch = options.component.branch || 'main';

    console.log('[OWASP Report] Project:', projectKey, 'Branch:', branch);

    // 使用 SonarQube 提供的容器元素
    const container = options.el;
    console.log('[OWASP Report] Using provided container:', container);

    // 設置容器樣式
    container.style.width = '100%';
    container.style.height = '100vh';
    container.style.overflow = 'auto';
    container.style.margin = '0';
    container.style.padding = '0';
    container.innerHTML = '<div style="padding: 20px; text-align: center;">Loading OWASP Report...</div>';

    // 載入報告 HTML 內容
    const reportUrl = `/static/aiowasp/report.html?project=${encodeURIComponent(projectKey)}&branch=${encodeURIComponent(branch)}`;
    console.log('[OWASP Report] Fetching report from:', reportUrl);

    fetch(reportUrl)
        .then(response => {
            console.log('[OWASP Report] Fetch response status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.text();
        })
        .then(html => {
            console.log('[OWASP Report] HTML loaded, length:', html.length);

            // 解析 HTML 文件
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            console.log('[OWASP Report] HTML parsed successfully');

            // 清空容器
            container.innerHTML = '';

            // 提取 head 中的 style 標籤
            const styles = doc.querySelectorAll('style');
            console.log('[OWASP Report] Found', styles.length, 'style elements');
            styles.forEach(style => {
                container.appendChild(style.cloneNode(true));
            });

            // 提取 body 內容
            const bodyContent = doc.body.innerHTML;
            console.log('[OWASP Report] Body content length:', bodyContent.length);

            // 創建內容容器
            const contentDiv = document.createElement('div');
            contentDiv.innerHTML = bodyContent;
            container.appendChild(contentDiv);

            console.log('[OWASP Report] Content div appended, children count:', contentDiv.children.length);

            // 不再嘗試執行內嵌 script - 已移除 CSP 違規代碼

            // 設置事件監聽器和初始化
            setTimeout(() => {
                console.log('[OWASP Report] Setting up event listeners and initializing...');
                setupEventListeners(container);
                initReport();
                console.log('[OWASP Report] Initialization complete');
            }, 100);

            console.log('[OWASP Report] Report displayed successfully');
        })
        .catch(error => {
            console.error('[OWASP Report] Error loading report:', error);
            container.innerHTML = `
                <div style="padding: 40px; text-align: center; color: #d32f2f;">
                    <h2>Error Loading OWASP Report</h2>
                    <p>${error.message}</p>
                    <p>Report URL: ${reportUrl}</p>
                </div>
            `;
        });

    console.log('[OWASP Report] Returning container element');

    // 返回容器元素
    return container;
});
