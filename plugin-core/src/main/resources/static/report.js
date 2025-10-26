// SonarQube Page Extension: OWASP Security Report
// 直接載入 HTML 內容而非使用 iframe

// ==================== 全域變數 ====================
let allFindings = [];
let filteredFindings = [];
let currentOwaspVersion = '2021';
let projectKey = 'unknown';

// ==================== 語法高亮輔助函數 ====================
/**
 * 根據檔案路徑偵測程式語言
 * @param {string} filePath - 檔案路徑
 * @returns {string} - Highlight.js 語言識別碼
 */
function detectLanguageFromPath(filePath) {
    if (!filePath) return 'plaintext';

    const ext = filePath.split('.').pop().toLowerCase();
    const languageMap = {
        'cs': 'csharp',
        'java': 'java',
        'js': 'javascript',
        'ts': 'typescript',
        'jsx': 'jsx',
        'tsx': 'tsx',
        'py': 'python',
        'rb': 'ruby',
        'go': 'go',
        'rs': 'rust',
        'cpp': 'cpp',
        'c': 'c',
        'h': 'c',
        'hpp': 'cpp',
        'php': 'php',
        'sql': 'sql',
        'xml': 'xml',
        'html': 'html',
        'css': 'css',
        'scss': 'scss',
        'json': 'json',
        'yaml': 'yaml',
        'yml': 'yaml',
        'sh': 'bash',
        'bash': 'bash'
    };

    return languageMap[ext] || 'plaintext';
}

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
                    <h3>${escapeHtml(finding.ruleName || finding.title)} <span class="expand-hint">▼ 點選展開詳細資訊</span></h3>
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
            ${finding.codeSnippet ? `
                <div class="original-code-section">
                    <div class="original-code-header">
                        <strong>📄 原始程式碼</strong>
                    </div>
                    <div class="original-code-snippet">
                        <pre><code class="language-${detectLanguageFromPath(finding.filePath)}">${escapeHtml(finding.codeSnippet)}</code></pre>
                    </div>
                </div>
            ` : ''}
            <div class="finding-tags">
                ${finding.tags ? finding.tags.map(tag => `<span class="tag">${escapeHtml(tag)}</span>`).join('') : ''}
            </div>
            <div class="finding-details">
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
                <div class="ai-suggestion-section" id="ai-suggestion-${index}">
                    ${!finding.aiSuggestion ? `
                        <button class="btn-ai-suggest" data-index="${index}">
                            🤖 取得 AI 修復建議
                        </button>
                    ` : `
                        <div class="ai-suggestion-result">
                            <h4>🤖 AI 修復建議</h4>
                            <div class="ai-suggestion-content">${escapeHtml(finding.aiSuggestion)}</div>
                            ${finding.aiTokensUsed ? `<div class="ai-meta">Token 使用量: ${finding.aiTokensUsed} | 處理時間: ${finding.aiProcessingTime}ms</div>` : ''}
                        </div>
                    `}
                </div>
            </div>
        </div>
    `).join('');

    // Add event listeners to finding cards
    const cards = document.querySelectorAll('.finding-card');
    console.log('Found', cards.length, 'finding cards to attach event listeners');

    cards.forEach((card, index) => {
        card.addEventListener('click', function(e) {
            console.log('Card', index, 'clicked, current expanded state:', this.classList.contains('expanded'));
            console.log('Click target:', e.target.tagName, e.target.className);

            // Don't expand if clicking on AI suggestion button
            if (!e.target.classList.contains('btn-ai-suggest')) {
                this.classList.toggle('expanded');
                const isExpanded = this.classList.contains('expanded');
                console.log('Card', index, 'new expanded state:', isExpanded);

                // Force re-render of details section
                const details = this.querySelector('.finding-details');
                if (details) {
                    console.log('Details section found, display:', window.getComputedStyle(details).display);
                }
            }
        });
    });

    // Add event listeners to AI suggestion buttons
    const aiButtons = document.querySelectorAll('.btn-ai-suggest');
    aiButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.stopPropagation(); // Prevent card expansion
            const index = parseInt(this.getAttribute('data-index'));
            requestAiSuggestion(index);
        });
    });

    // Apply syntax highlighting to all code blocks
    // Check if hljs is loaded, if not, wait for it
    const applyHighlighting = () => {
        if (typeof hljs !== 'undefined') {
            document.querySelectorAll('pre code').forEach(block => {
                hljs.highlightElement(block);
            });
            console.log('[OWASP Report] Syntax highlighting applied');
        } else {
            console.log('[OWASP Report] Waiting for Highlight.js to load...');
            setTimeout(applyHighlighting, 100);
        }
    };
    applyHighlighting();
}

// ==================== AI 建議功能 ====================

// Request AI suggestion for a specific finding
async function requestAiSuggestion(index) {
    const finding = filteredFindings[index];
    if (!finding) {
        console.error('找不到對應的 finding:', index);
        return;
    }

    const section = document.getElementById(`ai-suggestion-${index}`);
    if (!section) {
        console.error('找不到 AI 建議區塊:', index);
        return;
    }

    // Show loading state
    section.innerHTML = `
        <div class="ai-suggestion-loading">
            <div class="spinner"></div>
            <p>🤖 AI 正在分析中，請稍候...</p>
        </div>
    `;

    try {
        // 準備 API 請求參數
        const params = new URLSearchParams({
            code: finding.codeSnippet || finding.description || 'No code snippet available',
            owaspCategory: finding.owaspCategory || '',
            cweId: (finding.cweIds && finding.cweIds.length > 0 ? finding.cweIds[0] : finding.cweId) || '',
            language: detectLanguage(finding.filePath),
            fileName: finding.filePath || ''
        });

        console.log('[AI Suggestion] Requesting AI suggestion for finding:', index);
        console.log('[AI Suggestion] Request params:', params.toString());

        // 呼叫 AI 建議 API (使用 GET 避免 CSRF 問題)
        const response = await fetch(`/api/aiowasp/suggest?${params.toString()}`, {
            method: 'GET',
            credentials: 'same-origin' // 傳送 session cookie 進行認證
        });

        if (!response.ok) {
            throw new Error(`API 請求失敗: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();
        console.log('[AI Suggestion] API response:', data);

        if (!data.success) {
            throw new Error(data.error || 'AI 分析失敗');
        }

        // 儲存 AI 建議到 finding 物件
        finding.aiSuggestion = data.analysisResult;
        finding.aiTokensUsed = data.tokensUsed;
        finding.aiProcessingTime = data.processingTimeMs;

        // 顯示 AI 建議結果（格式化顯示）
        section.innerHTML = formatAiSuggestion(data);

        console.log('[AI Suggestion] AI suggestion displayed successfully');

    } catch (error) {
        console.error('[AI Suggestion] Error:', error);

        // 顯示錯誤訊息
        section.innerHTML = `
            <div class="ai-suggestion-error">
                <h4>❌ AI 建議取得失敗</h4>
                <p>${escapeHtml(error.message)}</p>
                <button class="btn-ai-suggest btn-retry" data-index="${index}">
                    🔄 重試
                </button>
            </div>
        `;

        // Add event listener to retry button
        const retryButton = section.querySelector('.btn-retry');
        if (retryButton) {
            retryButton.addEventListener('click', function(e) {
                e.stopPropagation();
                const idx = parseInt(this.getAttribute('data-index'));
                requestAiSuggestion(idx);
            });
        }
    }
}

// Detect programming language from file path
function detectLanguage(filePath) {
    if (!filePath) return 'unknown';

    const ext = filePath.split('.').pop().toLowerCase();
    const languageMap = {
        'java': 'java',
        'js': 'javascript',
        'jsx': 'javascript',
        'ts': 'javascript',
        'tsx': 'javascript',
        'py': 'python',
        'cs': 'csharp',
        'php': 'php',
        'rb': 'ruby',
        'go': 'go',
        'cpp': 'cpp',
        'c': 'c',
        'h': 'c',
        'hpp': 'cpp'
    };

    return languageMap[ext] || 'unknown';
}

// 格式化 AI 建議顯示
function formatAiSuggestion(data) {
    try {
        // 嘗試解析 analysisResult 為 JSON
        let suggestion;
        try {
            suggestion = JSON.parse(data.analysisResult);
        } catch (e) {
            // 如果不是 JSON 格式，直接顯示原始文字
            return `
                <div class="ai-suggestion-result">
                    <h4>🤖 AI 修復建議</h4>
                    <div class="ai-suggestion-content">${escapeHtml(data.analysisResult)}</div>
                    <div class="ai-meta">
                        Token 使用量: ${data.tokensUsed || 'N/A'} |
                        處理時間: ${data.processingTimeMs || 'N/A'}ms |
                        模型: ${data.modelUsed || 'N/A'}
                    </div>
                </div>
            `;
        }

        // 格式化顯示 JSON 結構的建議
        let html = '<div class="ai-suggestion-result"><h4>🤖 AI 修復建議</h4>';

        // 顯示整體摘要（如果有）
        if (suggestion.summary) {
            html += `
                <div class="ai-summary">
                    <h5>📋 整體分析</h5>
                    <p>${escapeHtml(suggestion.summary)}</p>
                </div>
            `;
        }

        // 顯示問題列表
        if (suggestion.issues && suggestion.issues.length > 0) {
            suggestion.issues.forEach((issue, index) => {
                html += `
                    <div class="ai-issue">
                        <div class="ai-issue-header">
                            <span class="severity-badge severity-${issue.severity?.toLowerCase() || 'info'}">
                                ${issue.severity || 'INFO'}
                            </span>
                            ${issue.owaspCategory ? `<span class="owasp-badge">${escapeHtml(issue.owaspCategory)}</span>` : ''}
                            ${issue.cweId ? `<span class="cwe-badge">${escapeHtml(issue.cweId)}</span>` : ''}
                        </div>

                        ${issue.description ? `
                            <div class="ai-description">
                                <strong>問題描述：</strong>
                                <p>${escapeHtml(issue.description)}</p>
                            </div>
                        ` : ''}

                        ${issue.fixSuggestion ? `
                            <div class="ai-fix">
                                <strong>🔧 修復建議：</strong>
                                <p>${escapeHtml(issue.fixSuggestion)}</p>
                            </div>
                        ` : ''}

                        ${issue.codeExample ? `
                            <div class="ai-code-example">
                                <div class="code-before">
                                    <strong>修復前：</strong>
                                    <pre><code>${escapeHtml(issue.codeExample.before)}</code></pre>
                                </div>
                                <div class="code-after">
                                    <strong>修復後：</strong>
                                    <pre><code>${escapeHtml(issue.codeExample.after)}</code></pre>
                                </div>
                            </div>
                        ` : ''}

                        ${issue.effortEstimate ? `
                            <div class="ai-effort">
                                <strong>⏱️ 預估工作量：</strong>
                                <span class="effort-badge">${escapeHtml(issue.effortEstimate)}</span>
                            </div>
                        ` : ''}
                    </div>
                `;
            });
        }

        // 顯示元數據
        html += `
            <div class="ai-meta">
                Token 使用量: ${data.tokensUsed || 'N/A'} |
                處理時間: ${data.processingTimeMs || 'N/A'}ms |
                模型: ${data.modelUsed || 'N/A'}
            </div>
        </div>
        `;

        return html;

    } catch (error) {
        console.error('[AI Suggestion] Format error:', error);
        // 發生錯誤時顯示原始內容
        return `
            <div class="ai-suggestion-result">
                <h4>🤖 AI 修復建議</h4>
                <div class="ai-suggestion-content">${escapeHtml(data.analysisResult)}</div>
                <div class="ai-meta">
                    Token 使用量: ${data.tokensUsed || 'N/A'} |
                    處理時間: ${data.processingTimeMs || 'N/A'}ms |
                    模型: ${data.modelUsed || 'N/A'}
                </div>
            </div>
        `;
    }
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

// 全域清理函數：當離開 OWASP Report 頁面時清理殘留內容
function cleanupOwaspReportGlobal() {
    // 查找並移除所有 OWASP Report 專屬容器
    const owaspContainers = document.querySelectorAll('[data-extension="aiowasp-report"]');
    owaspContainers.forEach(container => {
        console.log('[OWASP Report] Global cleanup: removing container', container.id);
        container.remove();
    });

    // 重置全域變數
    allFindings = [];
    filteredFindings = [];
    currentOwaspVersion = '2021';
    projectKey = 'unknown';
}

// 監聽 URL 變化，當離開 OWASP Report 頁面時自動清理
let lastUrl = location.href;
new MutationObserver(() => {
    const currentUrl = location.href;
    if (currentUrl !== lastUrl) {
        console.log('[OWASP Report] URL changed from', lastUrl, 'to', currentUrl);

        // 如果從 OWASP Report 頁面離開
        if (lastUrl.includes('/aiowasp/report') && !currentUrl.includes('/aiowasp/report')) {
            console.log('[OWASP Report] Leaving OWASP Report page, triggering cleanup');
            cleanupOwaspReportGlobal();
        }

        lastUrl = currentUrl;
    }
}).observe(document, {subtree: true, childList: true});

window.registerExtension('aiowasp/report', function (options) {
    console.log('[OWASP Report] Extension starting...', options);

    // 獲取專案 key 和其他參數
    projectKey = options.component.key;
    const branch = options.component.branch || 'main';

    console.log('[OWASP Report] Project:', projectKey, 'Branch:', branch);

    // 使用 SonarQube 提供的容器元素
    const parentContainer = options.el;
    console.log('[OWASP Report] Parent container:', parentContainer.tagName, 'Children:', parentContainer.children.length);

    // ⚠️ 關鍵修正：徹底清空父容器（移除所有非 OWASP 的內容）
    while (parentContainer.firstChild) {
        parentContainer.removeChild(parentContainer.firstChild);
    }
    console.log('[OWASP Report] Parent container cleared');

    // 重置全域變數
    allFindings = [];
    filteredFindings = [];
    currentOwaspVersion = '2021';

    // 創建專屬的子容器，避免與其他 extension 衝突
    const container = document.createElement('div');
    container.id = 'aiowasp-report-container';
    container.className = 'aiowasp-report-wrapper';
    container.setAttribute('data-extension', 'aiowasp-report');
    container.setAttribute('data-timestamp', Date.now());

    // 設置容器樣式
    container.style.width = '100%';
    container.style.minHeight = '100vh';
    container.style.overflow = 'auto';
    container.style.margin = '0';
    container.style.padding = '0';

    // 設置載入訊息
    container.innerHTML = '<div style="padding: 20px; text-align: center;">Loading OWASP Report...</div>';

    // 將專屬容器附加到父容器
    parentContainer.appendChild(container);

    console.log('[OWASP Report] Created dedicated container:', container.id);

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

            // ⚠️ 關鍵：再次徹底清空容器（防止 async 競爭條件）
            while (container.firstChild) {
                container.removeChild(container.firstChild);
            }
            container.innerHTML = '';
            console.log('[OWASP Report] Container re-cleared before rendering, children:', container.children.length);

            // 動態載入 Highlight.js CSS (本地資源)
            if (!document.querySelector('link[href*="highlight.js"]')) {
                const highlightCss = document.createElement('link');
                highlightCss.rel = 'stylesheet';
                highlightCss.href = '/static/aiowasp/lib/highlight.js/github-dark.min.css';
                document.head.appendChild(highlightCss);
                console.log('[OWASP Report] Highlight.js CSS loaded from local');
            }

            // 動態載入 Highlight.js JavaScript (本地資源)
            if (!document.querySelector('script[src*="highlight.js"]')) {
                const highlightJs = document.createElement('script');
                highlightJs.src = '/static/aiowasp/lib/highlight.js/highlight.min.js';
                document.head.appendChild(highlightJs);
                console.log('[OWASP Report] Highlight.js script loaded from local');
            }

            // 提取 head 中的 style 標籤
            const styles = doc.querySelectorAll('style');
            console.log('[OWASP Report] Found', styles.length, 'style elements');
            styles.forEach(style => {
                container.appendChild(style.cloneNode(true));
            });

            // 提取 body 內容
            const bodyContent = doc.body.innerHTML;
            console.log('[OWASP Report] Body content length:', bodyContent.length);

            // 創建內容容器並設置唯一 ID 以便調試
            const contentDiv = document.createElement('div');
            contentDiv.id = 'owasp-report-content-' + Date.now();
            contentDiv.innerHTML = bodyContent;
            container.appendChild(contentDiv);

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

    console.log('[OWASP Report] Returning parent container element');

    // 返回父容器元素（SonarQube 會管理生命週期）
    return parentContainer;
});
