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

// 當前選擇的 OWASP 版本
let currentOwaspVersion = '2021';

// 更新 OWASP 分類選項
function updateOwaspCategoryOptions() {
    const owaspFilter = document.getElementById('owaspFilter');
    const categories = OWASP_VERSIONS[currentOwaspVersion];

    // 保存當前選擇的值
    const currentValue = owaspFilter.value;

    // 清空選項並重新添加
    owaspFilter.innerHTML = '<option value="">所有分類</option>';

    Object.entries(categories).forEach(([key, label]) => {
        const option = document.createElement('option');
        option.value = key;
        option.textContent = label;
        owaspFilter.appendChild(option);
    });

    // 嘗試恢復之前的選擇
    if (currentValue && categories[currentValue]) {
        owaspFilter.value = currentValue;
    }

    console.log(\`[OWASP Report] Updated category options for version \${currentOwaspVersion}\`);
}

// 導出給全域使用
window.OWASP_VERSIONS = OWASP_VERSIONS;
window.updateOwaspCategoryOptions = updateOwaspCategoryOptions;
window.currentOwaspVersion = currentOwaspVersion;
