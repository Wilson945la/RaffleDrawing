// ===== App State =====
let currentUser = null;
let ws = null;
let isDrawing = false;
let generatedAccountId = '';

// ===== DOM Elements =====
const pages = {
    auth: document.getElementById('auth-page'),
    user: document.getElementById('user-page'),
    admin: document.getElementById('admin-page')
};

const loginForm = document.getElementById('login-form');
const drawBtn = document.getElementById('draw-btn');
const prizeModal = document.getElementById('prize-modal');
const toast = document.getElementById('toast');

// ===== API Helper =====
async function api(url, options = {}) {
    const res = await fetch(url, {
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', ...options.headers },
        ...options
    });
    return res.json();
}

function apiPost(url, body) {
    return api(url, { method: 'POST', body: JSON.stringify(body) });
}

function apiGet(url) {
    return api(url, { method: 'GET' });
}

function apiPut(url, body) {
    return api(url, { method: 'PUT', body: JSON.stringify(body) });
}

function apiDelete(url) {
    return api(url, { method: 'DELETE' });
}

// ===== Page Navigation =====
function showPage(name) {
    Object.values(pages).forEach(p => p.classList.add('hidden'));
    pages[name].classList.remove('hidden');
}

// ===== Auth — Account ID Mode =====

// Generate account ID from server
async function fetchAccountId() {
    const res = await apiGet('/api/user/generate-id');
    if (res.success) {
        generatedAccountId = res.data;
        document.getElementById('account-id-display').textContent = generatedAccountId;
    }
}

// Copy account ID to clipboard
document.getElementById('btn-copy-id').addEventListener('click', () => {
    if (!generatedAccountId) return;
    navigator.clipboard.writeText(generatedAccountId).then(() => {
        showToast('账号ID已复制: ' + generatedAccountId);
    }).catch(() => {
        // Fallback for older browsers
        const temp = document.createElement('textarea');
        temp.value = generatedAccountId;
        document.body.appendChild(temp);
        temp.select();
        document.execCommand('copy');
        document.body.removeChild(temp);
        showToast('账号ID已复制: ' + generatedAccountId);
    });
});

// Login form submit
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    // Determine which account ID to use
    const existingId = document.getElementById('existing-id').value.trim();
    const realName = loginForm.realName.value.trim();
    const accountId = existingId || generatedAccountId;

    if (!accountId) {
        showToast('账号ID加载中，请稍后再试');
        return;
    }

    // Confirmation dialog
    showConfirmDialog(
        '方尼建议您确认复制好自己的账号以便中奖后奖品的兑换哦~',
        async () => {
            const res = await apiPost('/api/user/login', {
                accountId: accountId,
                realName: realName || undefined
            });
            if (res.success) {
                onLogin(res.data);
            } else {
                showToast(res.message || '登录失败');
            }
        }
    );
});

function onLogin(user) {
    currentUser = user;
    if (user.admin) {
        document.getElementById('admin-name').textContent = user.realName;
        showPage('admin');
        loadAdminData();
    } else {
        document.getElementById('user-name').textContent = user.realName;
        showPage('user');
        loadUserData();
    }
    connectWebSocket();
}

document.getElementById('logout-btn').addEventListener('click', logout);
document.getElementById('admin-logout-btn').addEventListener('click', logout);

async function logout() {
    await apiPost('/api/user/logout', {});
    currentUser = null;
    if (ws) { ws.close(); ws = null; }
    showPage('auth');
}

// ===== WebSocket =====
function connectWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    ws = new WebSocket(`${protocol}//${window.location.host}/ws/raffle`);
    ws.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        if (msg.type === 'new_winner') {
            addWinnerToList(msg.userName, msg.prizeName, new Date(msg.time));
            showToast(`${msg.userName} 抽中了 ${msg.prizeName}!`);
        } else if (msg.type === 'event_update') {
            if (currentUser && !currentUser.admin) {
                loadRunningEvents();
            }
        } else if (msg.type === 'result_changed') {
            // Refresh results lists
            if (currentUser && currentUser.admin) {
                loadAdminResults();
            } else {
                loadWinnersList();
            }
        }
    };
}

let currentEventId = null;

// ===== User Page =====
async function loadUserData() {
    // Show event selection by default
    showEventSelectScreen();
}

// ===== Event Selection Screen =====
function showEventSelectScreen() {
    currentEventId = null;
    document.getElementById('event-select-screen').classList.remove('hidden');
    document.getElementById('event-raffle-screen').classList.add('hidden');
    loadRunningEvents();
}

async function loadRunningEvents() {
    const res = await apiGet('/api/raffle/running-events');
    const listDiv = document.getElementById('running-events-list');
    const noEvents = document.getElementById('no-events-msg');

    if (res.success && res.data.length > 0) {
        noEvents.classList.add('hidden');
        listDiv.innerHTML = res.data.map(e => `
            <div class="event-select-card" onclick="enterEvent(${e.id})">
                <div class="event-select-card-header">
                    <span class="event-select-title">${escapeHtml(e.title || '未命名活动')}</span>
                    <span class="event-select-badge">进行中</span>
                </div>
                <div class="event-select-time">${e.startTime} - ${e.endTime}</div>
                <div class="event-select-enter">点击进入 →</div>
            </div>
        `).join('');
    } else {
        listDiv.innerHTML = '';
        noEvents.classList.remove('hidden');
    }
}

// ===== Event Raffle Screen =====
window.enterEvent = async function(eventId) {
    currentEventId = eventId;
    document.getElementById('event-select-screen').classList.add('hidden');
    document.getElementById('event-raffle-screen').classList.remove('hidden');

    // Load event info and user data
    const [eventsRes, userRes] = await Promise.all([
        apiGet('/api/raffle/running-events'),
        apiGet('/api/user/me')
    ]);

    // Find the event
    const event = eventsRes.success ? eventsRes.data.find(e => e.id === eventId) : null;
    if (event) {
        document.getElementById('event-title-display').textContent = event.title || '幸运抽奖';
        document.getElementById('event-time').textContent = `${event.startTime} - ${event.endTime}`;
        document.getElementById('status-badge').classList.add('running');
        document.getElementById('status-badge').classList.remove('ended');
        document.getElementById('status-badge').textContent = '抽奖进行中';
    }

    // Update draw count for this event
    const drawCountRes = await apiGet('/api/user/draw-count/' + eventId);
    const drawCount = drawCountRes.success ? (drawCountRes.data || 0) : 0;
    document.getElementById('user-draw-count').textContent = drawCount;

    // Reset draw button state
    const drawBtn = document.getElementById('draw-btn');
    drawBtn.disabled = false;
    drawBtn.querySelector('.draw-text').innerHTML = '立即<br>抽奖';
    document.getElementById('draw-result').classList.add('hidden');
    isDrawing = false;

    // Check if user already won in this event
    const myRes = await apiGet('/api/raffle/my-result?eventId=' + eventId);
    if (myRes.success && myRes.data === true) {
        drawBtn.disabled = true;
        drawBtn.querySelector('.draw-text').innerHTML = '已<br>抽奖';
    }

    // Check draw count
    if (drawCount <= 0 && !drawBtn.disabled) {
        drawBtn.disabled = true;
        drawBtn.querySelector('.draw-text').innerHTML = '无<br>次数';
    }

    // Load event prizes
    loadEventPrizes(eventId);

    // Load winners
    loadWinnersList();
};

async function loadEventPrizes(eventId) {
    const res = await apiGet('/api/raffle/event/' + eventId + '/prizes-public');
    const grid = document.getElementById('event-prizes-grid');
    if (res.success && res.data.length > 0) {
        grid.innerHTML = res.data.map(p => `
            <div class="prize-card">
                <img class="prize-card-img" src="${p.prizeImage || '/images/logo.png'}" alt="${escapeHtml(p.prizeName)}">
                <div class="prize-card-name">${escapeHtml(p.prizeName)}</div>
                <div class="prize-card-remaining">剩余 ${p.remaining}</div>
            </div>
        `).join('');
    } else {
        grid.innerHTML = '<div style="text-align:center;color:var(--text-light);padding:20px;">暂无奖品</div>';
    }
}

async function loadWinnersList() {
    const resultsRes = await apiGet('/api/raffle/results');
    const winnersList = document.getElementById('winners-list');
    if (resultsRes.success && resultsRes.data.length > 0) {
        winnersList.innerHTML = resultsRes.data.map((r, i) => renderWinnerItem(i + 1, r.userName, r.prizeName, r.raffleTime)).join('');
    } else {
        winnersList.innerHTML = '<div class="empty-winners">暂无中奖记录</div>';
    }
}

document.getElementById('back-to-events-btn').addEventListener('click', () => {
    showEventSelectScreen();
});

function renderWinnerItem(rank, name, prize, time) {
    let rankClass = '';
    if (rank === 1) rankClass = 'top1';
    else if (rank === 2) rankClass = 'top2';
    else if (rank === 3) rankClass = 'top3';
    return `
        <div class="winner-item">
            <div class="winner-rank ${rankClass}">${rank}</div>
            <div class="winner-info">
                <div class="winner-name">${escapeHtml(name)}</div>
                <div class="winner-prize">${escapeHtml(prize)}</div>
            </div>
            <div class="winner-time">${time.split(' ')[1] || time}</div>
        </div>
    `;
}

function addWinnerToList(name, prize, time) {
    const winnersList = document.getElementById('winners-list');
    const empty = winnersList.querySelector('.empty-winners');
    if (empty) empty.remove();
    const timeStr = time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    const html = renderWinnerItem(1, name, prize, ' ' + timeStr);
    winnersList.insertAdjacentHTML('afterbegin', html);
    // Re-rank
    const items = winnersList.querySelectorAll('.winner-item');
    items.forEach((item, i) => {
        const rankEl = item.querySelector('.winner-rank');
        rankEl.textContent = i + 1;
        rankEl.className = 'winner-rank';
        if (i === 0) rankEl.classList.add('top1');
        else if (i === 1) rankEl.classList.add('top2');
        else if (i === 2) rankEl.classList.add('top3');
    });
}

// ===== Draw =====
drawBtn.addEventListener('click', async () => {
    if (isDrawing || drawBtn.disabled) return;
    if (!currentEventId) {
        showToast('请先选择活动');
        return;
    }
    isDrawing = true;
    drawBtn.disabled = true;

    // Animation
    const ring = document.querySelector('.machine-ring');
    ring.style.animationDuration = '0.5s';
    drawBtn.querySelector('.draw-text').innerHTML = '抽奖<br>中...';

    await new Promise(r => setTimeout(r, 2000));

    const res = await apiPost('/api/raffle/draw', { eventId: currentEventId });

    ring.style.animationDuration = '20s';
    isDrawing = false;

    if (res.success) {
        const resultDiv = document.getElementById('draw-result');
        document.getElementById('result-prize-name').textContent = res.data.prizeName;
        const img = document.getElementById('result-prize-img');
        if (res.data.prizeImage) {
            img.src = res.data.prizeImage;
            img.classList.remove('hidden');
        } else {
            img.classList.add('hidden');
        }
        resultDiv.classList.remove('hidden');
        drawBtn.querySelector('.draw-text').innerHTML = '已<br>中奖';

        // Update draw count
        const newCount = res.data.userDrawCount;
        document.getElementById('user-draw-count').textContent = newCount != null ? newCount : 0;

        showToast('恭喜您中奖了！');

        // Refresh prizes (remaining count changes)
        loadEventPrizes(currentEventId);
    } else {
        drawBtn.disabled = false;
        drawBtn.querySelector('.draw-text').innerHTML = '立即<br>抽奖';
        showToast(res.message || '抽奖失败');
    }
});

// ===== Admin Page =====
const adminNavBtns = document.querySelectorAll('.admin-nav-btn');
adminNavBtns.forEach(btn => {
    btn.addEventListener('click', () => {
        adminNavBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        document.querySelectorAll('.admin-panel').forEach(p => p.classList.add('hidden'));
        document.getElementById('panel-' + btn.dataset.panel).classList.remove('hidden');
    });
});

async function loadAdminData() {
    await loadAdminUsers();
    await loadAdminResults();
    await loadPrizeList();
    await loadEventInfo();
}

// Prize Management
async function loadPrizeList() {
    const res = await apiGet('/api/prize/list');
    const container = document.getElementById('prize-list');
    if (res.success && res.data.length > 0) {
        container.innerHTML = res.data.map(p => `
            <div class="prize-item" data-id="${p.id}">
                <img class="prize-item-img" src="${p.imageBase64 || '/images/logo.png'}" alt="">
                <div class="prize-item-info">
                    <div class="prize-item-name">${escapeHtml(p.name)}</div>
                    <div class="prize-item-desc">${escapeHtml(p.description || '')}</div>
                </div>
                <div class="prize-item-actions">
                    <button class="btn-icon" onclick="editPrize(${p.id}, '${escapeHtml(p.name)}', '${escapeHtml(p.description || '')}')">&#9998;</button>
                    <button class="btn-icon danger" onclick="deletePrize(${p.id})">&#128465;</button>
                </div>
            </div>
        `).join('');
    } else {
        container.innerHTML = '<div style="text-align:center;color:var(--text-light);padding:20px;">暂无奖品，点击上方按钮添加</div>';
    }
}

document.getElementById('add-prize-btn').addEventListener('click', () => {
    document.getElementById('modal-title').textContent = '添加奖品';
    document.getElementById('prize-id').value = '';
    document.getElementById('prize-name').value = '';
    document.getElementById('prize-desc').value = '';
    document.getElementById('prize-image').value = '';
    document.getElementById('prize-image-preview').innerHTML = '';
    prizeModal.classList.remove('hidden');
});

window.editPrize = function(id, name, desc) {
    document.getElementById('modal-title').textContent = '编辑奖品';
    document.getElementById('prize-id').value = id;
    document.getElementById('prize-name').value = name;
    document.getElementById('prize-desc').value = desc;
    document.getElementById('prize-image').value = '';
    document.getElementById('prize-image-preview').innerHTML = '';
    prizeModal.classList.remove('hidden');
};

window.deletePrize = async function(id) {
    if (!confirm('确定要删除这个奖品吗？')) return;
    const res = await apiPost('/api/prize/delete/' + id, {});
    if (res.success) {
        showToast('删除成功');
        loadPrizeList();
    } else {
        showToast(res.message || '删除失败');
    }
};

document.querySelector('.modal-close').addEventListener('click', () => {
    prizeModal.classList.add('hidden');
});
document.querySelector('.modal-overlay').addEventListener('click', () => {
    prizeModal.classList.add('hidden');
});

document.getElementById('prize-image').addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = (ev) => {
            document.getElementById('prize-image-preview').innerHTML = `<img src="${ev.target.result}" alt="">`;
        };
        reader.readAsDataURL(file);
    }
});

document.getElementById('prize-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('prize-id').value;
    const formData = new FormData();
    formData.append('name', document.getElementById('prize-name').value);
    formData.append('description', document.getElementById('prize-desc').value);
    const fileInput = document.getElementById('prize-image');
    if (fileInput.files[0]) {
        formData.append('image', fileInput.files[0]);
    }

    const url = id ? '/api/prize/update/' + id : '/api/prize/create';
    const res = await fetch(url, {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
    }).then(r => r.json());

    if (res.success) {
        showToast(id ? '更新成功' : '添加成功');
        prizeModal.classList.add('hidden');
        loadPrizeList();
    } else {
        showToast(res.message || '操作失败');
    }
});

// Event Settings
async function loadEventInfo() {
    const res = await apiGet('/api/raffle/events');
    const container = document.getElementById('event-list');
    if (res.success && res.data.length > 0) {
        container.innerHTML = res.data.map(e => {
            const statusClass = e.running ? 'event-running' : (e.active ? 'event-active' : 'event-inactive');
            const statusText = e.running ? '● 进行中' : (e.active ? '○ 待开始' : '○ 已停用');
            return `
            <div class="event-card ${statusClass}">
                <div class="event-card-header">
                    <span class="event-card-title">${escapeHtml(e.title || '未命名活动')}</span>
                    <span class="event-card-status">${statusText}</span>
                </div>
                <div class="event-card-time">${e.startTime} - ${e.endTime}</div>
                <div class="event-card-actions">
                    ${e.active ? `<button class="btn btn-small btn-deactivate" onclick="deactivateEvent(${e.id})">停用</button>` : `<button class="btn btn-small btn-activate" onclick="activateEvent(${e.id})">启用</button>`}
                    <button class="btn btn-small" onclick="editEvent(${e.id})">编辑</button>
                    <button class="btn btn-small danger" onclick="deleteEvent(${e.id})">删除</button>
                </div>
            </div>
            `;
        }).join('');
    } else {
        container.innerHTML = '<div style="text-align:center;color:var(--text-light);padding:20px;">暂无活动，点击上方按钮创建</div>';
    }
}

// ===== Event Modal =====
const eventModal = document.getElementById('event-modal');

document.getElementById('add-event-btn').addEventListener('click', () => {
    openEventModal();
});

function openEventModal(eventData) {
    document.getElementById('event-id').value = eventData ? eventData.id : '';
    document.getElementById('event-modal-title').textContent = eventData ? '编辑活动' : '新建活动';
    document.getElementById('event-title').value = eventData ? eventData.title : '';

    if (eventData) {
        document.getElementById('event-start').value = eventData.startTime.replace(' ', 'T').substring(0, 16);
        document.getElementById('event-end').value = eventData.endTime.replace(' ', 'T').substring(0, 16);
    } else {
        document.getElementById('event-start').value = '';
        document.getElementById('event-end').value = '';
    }

    // Load prize configs
    const configContainer = document.getElementById('event-prizes-config');
    configContainer.innerHTML = '';

    if (eventData) {
        // Load existing prize configs for this event
        loadEventPrizeConfigs(eventData.id);
    }

    eventModal.classList.remove('hidden');
}

async function loadEventPrizeConfigs(eventId) {
    const configContainer = document.getElementById('event-prizes-config');
    const res = await apiGet('/api/raffle/event/' + eventId + '/prizes');
    const allPrizesRes = await apiGet('/api/prize/list');

    configContainer.innerHTML = '';

    if (res.success && res.data.length > 0) {
        res.data.forEach(ep => {
            addPrizeConfigRow(allPrizesRes.success ? allPrizesRes.data : [], ep.prizeId, ep.quantity, ep.probability);
        });
    }
}

// Add a prize configuration row
function addPrizeConfigRow(allPrizes, selectedPrizeId, quantity, probability) {
    const configContainer = document.getElementById('event-prizes-config');
    const row = document.createElement('div');
    row.className = 'event-prize-row';

    const prizeOptions = allPrizes.map(p =>
        `<option value="${p.id}" ${p.id === selectedPrizeId ? 'selected' : ''}>${escapeHtml(p.name)}</option>`
    ).join('');

    row.innerHTML = `
        <select class="ep-prize-select" required>
            <option value="">选择奖品</option>
            ${prizeOptions}
        </select>
        <input type="number" class="ep-quantity" min="1" value="${quantity || 1}" placeholder="数量" required>
        <input type="number" class="ep-probability" min="0" max="100" step="0.1" value="${probability != null ? (probability * 100).toFixed(1) : '0'}" placeholder="概率%" required>
        <button type="button" class="btn-icon danger ep-remove-btn" title="移除">&#128465;</button>
    `;

    row.querySelector('.ep-remove-btn').addEventListener('click', () => row.remove());
    configContainer.appendChild(row);
}

document.getElementById('add-event-prize-btn').addEventListener('click', async () => {
    const res = await apiGet('/api/prize/list');
    if (res.success) {
        addPrizeConfigRow(res.data, null, 1, 0);
    }
});

// Save event
document.getElementById('event-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('event-id').value;
    const title = document.getElementById('event-title').value.trim();
    const startTime = document.getElementById('event-start').value;
    const endTime = document.getElementById('event-end').value;

    if (!title || !startTime || !endTime) {
        showToast('请填写完整信息');
        return;
    }

    // Collect prize configs
    const prizeRows = document.querySelectorAll('.event-prize-row');
    const prizes = [];
    prizeRows.forEach(row => {
        const select = row.querySelector('.ep-prize-select');
        const qty = row.querySelector('.ep-quantity');
        const prob = row.querySelector('.ep-probability');
        if (select.value) {
            prizes.push({
                prizeId: parseInt(select.value),
                quantity: parseInt(qty.value) || 1,
                probability: parseFloat(prob.value) / 100 || 0
            });
        }
    });

    const body = { title, startTime, endTime, prizes };

    let res;
    if (id) {
        res = await apiPut('/api/raffle/event/update/' + id, body);
    } else {
        res = await apiPost('/api/raffle/event/create', body);
    }

    if (res.success) {
        showToast(id ? '活动更新成功' : '活动创建成功');
        eventModal.classList.add('hidden');
        loadEventInfo();
    } else {
        showToast(res.message || '操作失败');
    }
});

window.editEvent = async function(eventId) {
    // Load event detail and open modal
    const eventsRes = await apiGet('/api/raffle/events');
    if (eventsRes.success) {
        const event = eventsRes.data.find(e => e.id === eventId);
        if (event) {
            openEventModal(event);
        }
    }
};

window.deleteEvent = async function(eventId) {
    if (!confirm('确定要删除这个活动吗？相关奖品配置也将被删除。')) return;
    const res = await apiDelete('/api/raffle/event/delete/' + eventId);
    if (res.success) {
        showToast('删除成功');
        loadEventInfo();
    } else {
        showToast(res.message || '删除失败');
    }
};

window.activateEvent = async function(eventId) {
    if (!confirm('确定要启用这个活动吗？')) return;
    const res = await apiPut('/api/raffle/event/activate/' + eventId, {});
    if (res.success) {
        showToast('活动已启用');
        loadEventInfo();
    } else {
        showToast(res.message || '操作失败');
    }
};

window.deactivateEvent = async function(eventId) {
    if (!confirm('确定要停用这个活动吗？已抽奖记录不受影响。')) return;
    const res = await apiPut('/api/raffle/event/deactivate/' + eventId, {});
    if (res.success) {
        showToast('活动已停用');
        loadEventInfo();
    } else {
        showToast(res.message || '操作失败');
    }
};

// Event Modal Close
document.querySelectorAll('#event-modal .modal-close, #event-modal .modal-overlay').forEach(el => {
    el.addEventListener('click', () => {
        eventModal.classList.add('hidden');
    });
});

// Admin Results
async function loadAdminResults() {
    const keyword = document.getElementById('result-search')?.value?.trim() || '';
    const url = keyword ? `/api/raffle/results?keyword=${encodeURIComponent(keyword)}` : '/api/raffle/results';
    const res = await apiGet(url);
    const container = document.getElementById('admin-results-list');
    if (res.success && res.data.length > 0) {
        container.innerHTML = res.data.map(r => {
            const isProcessed = r.processed === true;
            return `
            <div class="result-item ${isProcessed ? 'result-processed' : ''}">
                <div class="result-item-info">
                    <div class="result-item-line">
                        <span class="result-account-id">${escapeHtml(r.accountId)}</span>
                        ${r.eventTitle ? `<span class="result-event-tag">${escapeHtml(r.eventTitle)}</span>` : ''}
                    </div>
                    <div class="result-item-line">
                        <span class="result-user-name">${escapeHtml(r.userName || '未署名')}</span>
                        <span class="result-arrow">→</span>
                        <span class="result-prize-name">${escapeHtml(r.prizeName)}</span>
                    </div>
                    <div class="result-item-time">${r.raffleTime}</div>
                </div>
                ${isProcessed ? `
                <div class="result-processed-badge">✓ 已处理</div>
                ` : `
                <div class="result-item-actions">
                    <div class="result-item-edit-row">
                        <button class="btn-icon" onclick="editResult(${r.id}, ${r.userId}, ${r.prizeId}, '${r.raffleTime}')" title="编辑">&#9998;</button>
                        <button class="btn-icon danger" onclick="deleteResult(${r.id})" title="删除">&#128465;</button>
                    </div>
                    <button class="btn btn-small btn-process" onclick="processResult(${r.id}, '${escapeHtml(r.accountId)}', '${escapeHtml(r.userName || '未署名')}', '${escapeHtml(r.prizeName)}')">确 认</button>
                </div>
                `}
            </div>
            `;
        }).join('');
    } else {
        container.innerHTML = '<div style="text-align:center;color:var(--text-light);padding:20px;">暂无中奖记录</div>';
    }
}

// Search debounce for results
let resultSearchTimer = null;
document.getElementById('result-search')?.addEventListener('input', () => {
    clearTimeout(resultSearchTimer);
    resultSearchTimer = setTimeout(loadAdminResults, 300);
});

// Admin User Management
async function loadAdminUsers() {
    const keyword = document.getElementById('user-search')?.value?.trim() || '';
    const url = keyword ? `/api/user/list?keyword=${encodeURIComponent(keyword)}` : '/api/user/list';
    const res = await apiGet(url);
    const container = document.getElementById('user-list');
    if (res.success && res.data.length > 0) {
        container.innerHTML = res.data.map(u => `
            <div class="user-item">
                <div class="user-item-info">
                    <div class="user-item-name">${escapeHtml(u.realName || '未署名')}</div>
                    <div class="user-item-account">${escapeHtml(u.accountId)}</div>
                </div>
                <div class="user-item-actions">
                    <button class="btn btn-small btn-draw-add" onclick="addUserDrawCount(${u.id}, '${escapeHtml(u.realName || '未署名')}', '${escapeHtml(u.accountId)}')">+1 次数</button>
                </div>
            </div>
        `).join('');
    } else {
        container.innerHTML = '<div style="text-align:center;color:var(--text-light);padding:20px;">暂无用户</div>';
    }
}

// Search debounce for users
let userSearchTimer = null;
document.getElementById('user-search')?.addEventListener('input', () => {
    clearTimeout(userSearchTimer);
    userSearchTimer = setTimeout(loadAdminUsers, 300);
});

window.addUserDrawCount = async function(userId, realName, accountId) {
    // Load running events for selection
    const eventsRes = await apiGet('/api/raffle/running-events');
    const events = eventsRes.success ? eventsRes.data : [];
    if (events.length === 0) {
        showToast('暂无正在进行的活动');
        return;
    }

    // Remove existing picker if any
    const existing = document.querySelector('.event-picker-overlay');
    if (existing) existing.remove();

    const options = events.map(e => 
        `<option value="${e.id}">${escapeHtml(e.title || '未命名活动')}</option>`
    ).join('');

    const overlay = document.createElement('div');
    overlay.className = 'event-picker-overlay';
    overlay.innerHTML = `
        <div class="event-picker-box">
            <div class="cfm-icon"><img src="images/robot.png" alt="方尼" class="fangni-robot"></div>
            <div class="event-picker-title">为用户「${escapeHtml(realName)}」(${escapeHtml(accountId)}) 增加抽奖次数</div>
            <div class="event-picker-select-row">
                <label>选择活动：</label>
                <select id="event-picker-select" class="event-picker-select">
                    ${options}
                </select>
            </div>
            <div class="cfm-btns">
                <button class="cfm-cancel">取消</button>
                <button class="cfm-ok">确认</button>
            </div>
        </div>
    `;
    document.body.appendChild(overlay);

    overlay.querySelector('.cfm-cancel').addEventListener('click', () => overlay.remove());
    overlay.querySelector('.cfm-ok').addEventListener('click', async () => {
        const select = document.getElementById('event-picker-select');
        const selectedOption = select.options[select.selectedIndex];
        const eventId = select.value;
        const eventTitle = selectedOption.text;

        overlay.remove();

        // Second confirm
        showConfirmDialog(
            `确定要为「${realName}」增加一次「${eventTitle}」的抽奖次数吗？`,
            async () => {
                const res = await apiPost('/api/user/add-draw-count', {
                    userId: userId,
                    eventId: parseInt(eventId)
                });
                if (res.success) {
                    showToast(`已为「${realName}」在「${res.data.eventTitle}」增加一次抽奖次数（当前共 ${res.data.drawCount} 次）`);
                    loadAdminUsers();
                } else {
                    showToast(res.message || '操作失败');
                }
            }
        );
    });
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) overlay.remove();
    });
};

window.editResult = async function(resultId, userId, prizeId, raffleTime) {
    // Load users and prizes for dropdowns
    const [usersRes, prizesRes] = await Promise.all([
        apiGet('/api/user/list'),
        apiGet('/api/prize/list')
    ]);

    const userSelect = document.getElementById('edit-result-user');
    if (usersRes.success) {
        userSelect.innerHTML = '<option value="">请选择用户</option>' +
            usersRes.data.map(u => `<option value="${u.id}" ${u.id === userId ? 'selected' : ''}>${escapeHtml(u.realName || '未署名')} (${escapeHtml(u.accountId)})</option>`).join('');
    }

    const prizeSelect = document.getElementById('edit-result-prize');
    if (prizesRes.success) {
        prizeSelect.innerHTML = '<option value="">请选择奖品</option>' +
            prizesRes.data.map(p => `<option value="${p.id}" ${p.id === prizeId ? 'selected' : ''}>${escapeHtml(p.name)}</option>`).join('');
    }

    // Format datetime-local value
    document.getElementById('edit-result-time').value = raffleTime.replace(' ', 'T').substring(0, 16);
    document.getElementById('edit-result-id').value = resultId;
    document.getElementById('result-modal').classList.remove('hidden');
};

window.deleteResult = async function(id) {
    if (!confirm('确定要删除这条中奖记录吗？该操作会归还奖品库存。')) return;
    const res = await apiDelete('/api/raffle/result/delete/' + id);
    if (res.success) {
        showToast('删除成功');
        loadAdminResults();
        loadPrizeList();
    } else {
        showToast(res.message || '删除失败');
    }
};

window.processResult = function(resultId, accountId, userName, prizeName) {
    showConfirmDialog(
        `方尼提醒您：是否确认该条奖励记录已兑换？<br><br><b>${escapeHtml(accountId)}</b><br>${escapeHtml(userName)} → ${escapeHtml(prizeName)}`,
        async () => {
            const res = await apiPut('/api/raffle/result/process/' + resultId);
            if (res.success) {
                showToast('记录已标记为已处理');
                loadAdminResults();
            } else {
                showToast(res.message || '操作失败');
            }
        }
    );
};

// Result Edit Form Submit
document.getElementById('result-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('edit-result-id').value;
    const userId = parseInt(document.getElementById('edit-result-user').value);
    const prizeId = parseInt(document.getElementById('edit-result-prize').value);
    const raffleTime = document.getElementById('edit-result-time').value;

    if (!userId || !prizeId) {
        showToast('请选择用户和奖品');
        return;
    }

    const res = await apiPut('/api/raffle/result/update/' + id, {
        userId: userId,
        prizeId: prizeId,
        raffleTime: raffleTime
    });

    if (res.success) {
        showToast('修改成功');
        document.getElementById('result-modal').classList.add('hidden');
        loadAdminResults();
        loadPrizeList();
    } else {
        showToast(res.message || '修改失败');
    }
});

// Result Modal Close
document.querySelectorAll('#result-modal .modal-close, #result-modal .modal-overlay').forEach(el => {
    el.addEventListener('click', () => {
        document.getElementById('result-modal').classList.add('hidden');
    });
});

// ===== Utilities =====
function showToast(message) {
    toast.textContent = message;
    toast.classList.remove('hidden');
    setTimeout(() => toast.classList.add('hidden'), 3000);
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ===== Confirm Dialog =====
function showConfirmDialog(message, onConfirm) {
    // Remove existing dialog if any
    const existing = document.querySelector('.confirm-overlay');
    if (existing) existing.remove();

    const overlay = document.createElement('div');
    overlay.className = 'confirm-overlay';
    overlay.innerHTML = `
        <div class="confirm-box">
            <div class="cfm-icon"><img src="images/robot.png" alt="方尼" class="fangni-robot"></div>
            <div class="cfm-msg">${escapeHtml(message)}</div>
            <div class="cfm-btns">
                <button class="cfm-cancel">取消</button>
                <button class="cfm-ok">确认</button>
            </div>
        </div>
    `;
    document.body.appendChild(overlay);

    overlay.querySelector('.cfm-cancel').addEventListener('click', () => overlay.remove());
    overlay.querySelector('.cfm-ok').addEventListener('click', () => {
        overlay.remove();
        if (onConfirm) onConfirm();
    });
    // Click overlay background to close
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) overlay.remove();
    });
}

// ===== Init =====
async function init() {
    // Load the generated account ID from server
    await fetchAccountId();

    // Check if already logged in
    const res = await apiGet('/api/user/me');
    if (res.success && res.data.id) {
        onLogin(res.data);
    } else {
        showPage('auth');
    }
}

init();
