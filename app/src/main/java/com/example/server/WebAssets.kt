package com.example.server

object WebAssets {
    val INDEX_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Local Inbox Web Access</title>
    <style>
        :root {
            --bg: #0f172a;
            --surface: #1e293b;
            --surface-card: #334155;
            --primary: #6366f1;
            --primary-hover: #4f46e5;
            --text: #f8fafc;
            --text-muted: #94a3b8;
            --border: #475569;
            --danger: #ef4444;
            --success: #22c55e;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, -apple-system, sans-serif; }
        body { background: var(--bg); color: var(--text); padding: 24px; min-height: 100vh; }
        .container { max-width: 1100px; margin: 0 auto; }
        
        header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--border); }
        .logo { font-size: 24px; font-weight: 700; color: var(--text); display: flex; align-items: center; gap: 10px; }
        .logo span { background: var(--primary); color: white; padding: 4px 10px; border-radius: 8px; font-size: 14px; }

        .auth-box { max-width: 400px; margin: 100px auto; background: var(--surface); padding: 32px; border-radius: 16px; border: 1px solid var(--border); text-align: center; }
        .auth-box h2 { margin-bottom: 12px; }
        .auth-box input { width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border); background: var(--bg); color: var(--text); margin: 16px 0; font-size: 18px; text-align: center; letter-spacing: 4px; }

        .btn { background: var(--primary); color: white; border: none; padding: 10px 18px; border-radius: 8px; font-weight: 600; cursor: pointer; transition: 0.2s; display: inline-flex; align-items: center; gap: 8px; }
        .btn:hover { background: var(--primary-hover); }
        .btn-outline { background: transparent; border: 1px solid var(--border); color: var(--text); }
        .btn-outline:hover { background: var(--surface-card); }
        .btn-danger { background: var(--danger); }

        .dashboard-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; margin-bottom: 24px; }
        .card { background: var(--surface); padding: 20px; border-radius: 12px; border: 1px solid var(--border); }
        .card-title { color: var(--text-muted); font-size: 13px; margin-bottom: 8px; font-weight: 600; text-transform: uppercase; }
        .card-value { font-size: 22px; font-weight: 700; }

        .actions-bar { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; align-items: center; justify-content: space-between; }
        .search-box { flex: 1; min-width: 250px; position: relative; }
        .search-box input { width: 100%; padding: 12px 16px; border-radius: 10px; border: 1px solid var(--border); background: var(--surface); color: var(--text); font-size: 15px; }

        .filter-chips { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 8px; margin-bottom: 20px; }
        .chip { background: var(--surface); border: 1px solid var(--border); padding: 6px 14px; border-radius: 20px; font-size: 13px; cursor: pointer; white-space: nowrap; }
        .chip.active { background: var(--primary); border-color: var(--primary); }

        .dropzone { border: 2px dashed var(--primary); background: rgba(99, 102, 241, 0.05); padding: 30px; border-radius: 16px; text-align: center; margin-bottom: 24px; cursor: pointer; transition: 0.2s; }
        .dropzone:hover, .dropzone.dragover { background: rgba(99, 102, 241, 0.15); }

        .items-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
        .item-card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 16px; display: flex; flex-direction: column; justify-content: space-between; gap: 12px; position: relative; }
        .item-type { font-size: 11px; font-weight: 700; padding: 3px 8px; border-radius: 6px; background: var(--surface-card); color: var(--primary); display: inline-block; width: max-content; }
        .item-title { font-size: 16px; font-weight: 600; line-height: 1.3; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
        .item-meta { font-size: 12px; color: var(--text-muted); display: flex; justify-content: space-between; align-items: center; }
        
        .preview-media { width: 100%; max-height: 200px; object-fit: cover; border-radius: 8px; margin-top: 8px; }
        audio, video { width: 100%; border-radius: 8px; margin-top: 8px; }

        .modal { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.7); align-items: center; justify-content: center; z-index: 100; }
        .modal.active { display: flex; }
        .modal-content { background: var(--surface); padding: 24px; border-radius: 16px; width: 100%; max-width: 500px; border: 1px solid var(--border); }
        .modal-content textarea, .modal-content input { width: 100%; padding: 10px; margin: 10px 0; border-radius: 8px; border: 1px solid var(--border); background: var(--bg); color: var(--text); }

        #uploadInput { display: none; }
    </style>
</head>
<body>
    <div id="authScreen" class="auth-box">
        <h2>Local Inbox Access</h2>
        <p style="color: var(--text-muted); font-size: 14px;">Enter the 6-digit PIN displayed on your Android phone screen.</p>
        <input type="password" id="pinInput" maxlength="6" placeholder="• • • • • •" />
        <button class="btn" style="width: 100%; justify-content: center;" onclick="login()">Unlock Session</button>
    </div>

    <div id="appScreen" class="container" style="display: none;">
        <header>
            <div class="logo">
                Local Inbox <span>LAN</span>
            </div>
            <div>
                <button class="btn btn-outline" onclick="logout()">Logout</button>
            </div>
        </header>

        <div class="dashboard-grid">
            <div class="card">
                <div class="card-title">Device Storage</div>
                <div class="card-value" id="deviceStorageText">Loading...</div>
            </div>
            <div class="card">
                <div class="card-title">Inbox Usage</div>
                <div class="card-value" id="inboxStorageText">Loading...</div>
            </div>
            <div class="card">
                <div class="card-title">Total Inbox Items</div>
                <div class="card-value" id="itemCountText">0</div>
            </div>
        </div>

        <div class="dropzone" id="dropzone" onclick="document.getElementById('uploadInput').click()">
            <h3 style="margin-bottom: 6px;">📁 Drop files here or click to upload</h3>
            <p style="color: var(--text-muted); font-size: 13px;">Supports Images, Videos, Audio, PDFs, Documents, and Generic Files</p>
            <input type="file" id="uploadInput" multiple onchange="handleFileUpload(this.files)" />
        </div>

        <div class="actions-bar">
            <div class="search-box">
                <input type="text" id="searchInput" placeholder="Search inbox items..." oninput="fetchItems()" />
            </div>
            <div style="display: flex; gap: 8px;">
                <button class="btn" onclick="openNoteModal()">+ New Note</button>
                <button class="btn btn-outline" onclick="openUrlModal()">+ Add URL</button>
            </div>
        </div>

        <div class="filter-chips">
            <div class="chip active" onclick="setFilter('', this)">All</div>
            <div class="chip" onclick="setFilter('TEXT', this)">Text</div>
            <div class="chip" onclick="setFilter('URL', this)">Links</div>
            <div class="chip" onclick="setFilter('IMAGE', this)">Images</div>
            <div class="chip" onclick="setFilter('AUDIO', this)">Audio</div>
            <div class="chip" onclick="setFilter('VIDEO', this)">Video</div>
            <div class="chip" onclick="setFilter('PDF', this)">PDF</div>
            <div class="chip" onclick="setFilter('FILE', this)">Files</div>
        </div>

        <div class="items-grid" id="itemsGrid"></div>
    </div>

    <!-- Note Modal -->
    <div class="modal" id="noteModal">
        <div class="modal-content">
            <h3>Add New Note</h3>
            <textarea id="noteContent" rows="6" placeholder="Type or paste your note text here..."></textarea>
            <div style="display: flex; justify-content: flex-end; gap: 8px;">
                <button class="btn btn-outline" onclick="closeModal('noteModal')">Cancel</button>
                <button class="btn" onclick="submitNote()">Save Note</button>
            </div>
        </div>
    </div>

    <!-- URL Modal -->
    <div class="modal" id="urlModal">
        <div class="modal-content">
            <h3>Add Link</h3>
            <input type="url" id="urlContent" placeholder="https://example.com/article" />
            <div style="display: flex; justify-content: flex-end; gap: 8px;">
                <button class="btn btn-outline" onclick="closeModal('urlModal')">Cancel</button>
                <button class="btn" onclick="submitUrl()">Save Link</button>
            </div>
        </div>
    </div>

    <script>
        let currentFilter = '';
        let authToken = localStorage.getItem('inbox_token') || '';

        window.onload = function() {
            if (authToken) {
                checkAuthAndLoad();
            }
        };

        async function login() {
            const pin = document.getElementById('pinInput').value;
            const res = await fetch('/api/login', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({pin})
            });
            const data = await res.json();
            if (data.status === 'ok') {
                authToken = data.token;
                localStorage.setItem('inbox_token', authToken);
                checkAuthAndLoad();
            } else {
                alert('Invalid PIN');
            }
        }

        function logout() {
            localStorage.removeItem('inbox_token');
            authToken = '';
            document.getElementById('authScreen').style.display = 'block';
            document.getElementById('appScreen').style.display = 'none';
        }

        async function checkAuthAndLoad() {
            const res = await fetch('/api/status', {
                headers: {'X-Auth-Token': authToken}
            });
            if (res.status === 401) {
                logout();
                return;
            }
            document.getElementById('authScreen').style.display = 'none';
            document.getElementById('appScreen').style.display = 'block';
            loadStats();
            fetchItems();
        }

        async function loadStats() {
            const [storageRes, deviceRes] = await Promise.all([
                fetch('/api/storage', { headers: {'X-Auth-Token': authToken} }),
                fetch('/api/device', { headers: {'X-Auth-Token': authToken} })
            ]);
            if (storageRes.ok) {
                const s = await storageRes.json();
                document.getElementById('deviceStorageText').innerText = `${'$'}{formatBytes(s.usedBytes)} / ${'$'}{formatBytes(s.totalBytes)}`;
                document.getElementById('inboxStorageText').innerText = formatBytes(s.inboxSize || 0);
            }
        }

        async function fetchItems() {
            const q = document.getElementById('searchInput').value;
            let url = `/api/items?q=${'$'}{encodeURIComponent(q)}`;
            if (currentFilter) url += `&type=${'$'}{currentFilter}`;

            const res = await fetch(url, { headers: {'X-Auth-Token': authToken} });
            if (!res.ok) return;
            const items = await res.json();
            document.getElementById('itemCountText').innerText = items.length;
            renderItems(items);
        }

        function setFilter(type, el) {
            currentFilter = type;
            document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
            el.classList.add('active');
            fetchItems();
        }

        function renderItems(items) {
            const grid = document.getElementById('itemsGrid');
            if (items.length === 0) {
                grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 40px;">No items in inbox</div>`;
                return;
            }

            grid.innerHTML = items.map(item => {
                let contentHtml = '';
                if (item.type === 'IMAGE') {
                    contentHtml = `<img src="/api/files/${'$'}{item.id}" class="preview-media" alt="${'$'}{item.title}" />`;
                } else if (item.type === 'AUDIO') {
                    contentHtml = `<audio controls src="/api/files/${'$'}{item.id}"></audio>`;
                } else if (item.type === 'VIDEO') {
                    contentHtml = `<video controls src="/api/files/${'$'}{item.id}"></video>`;
                } else if (item.type === 'TEXT') {
                    contentHtml = `<p style="font-size: 14px; color: var(--text-muted); line-height: 1.4;">${'$'}{escapeHtml(item.text)}</p>`;
                } else if (item.type === 'URL') {
                    contentHtml = `<a href="${'$'}{item.url}" target="_blank" style="color: var(--primary); word-break: break-all;">${'$'}{item.url}</a>`;
                }

                return `
                    <div class="item-card">
                        <div>
                            <span class="item-type">${'$'}{item.type}</span>
                            <div class="item-title" style="margin-top: 8px;">${'$'}{escapeHtml(item.title)}</div>
                            ${'$'}{contentHtml}
                        </div>
                        <div class="item-meta">
                            <span>${'$'}{formatDate(item.createdAt)}</span>
                            <div>
                                ${'$'}{item.localFilePath ? `<a class="btn btn-outline" style="padding: 4px 8px; font-size: 12px;" href="/api/files/${'$'}{item.id}?download=true">Download</a>` : ''}
                                <button class="btn btn-danger" style="padding: 4px 8px; font-size: 12px;" onclick="deleteItem(${'$'}{item.id})">Delete</button>
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
        }

        async function handleFileUpload(files) {
            for (let file of files) {
                const formData = new FormData();
                formData.append('file', file);
                await fetch('/api/upload', {
                    method: 'POST',
                    headers: {'X-Auth-Token': authToken},
                    body: formData
                });
            }
            fetchItems();
            loadStats();
        }

        async function deleteItem(id) {
            if (!confirm('Delete this inbox item?')) return;
            await fetch(`/api/items/${'$'}{id}`, {
                method: 'DELETE',
                headers: {'X-Auth-Token': authToken}
            });
            fetchItems();
            loadStats();
        }

        function openNoteModal() { document.getElementById('noteModal').classList.add('active'); }
        function openUrlModal() { document.getElementById('urlModal').classList.add('active'); }
        function closeModal(id) { document.getElementById(id).classList.remove('active'); }

        async function submitNote() {
            const text = document.getElementById('noteContent').value;
            if (!text) return;
            await fetch('/api/items/text', {
                method: 'POST',
                headers: {'Content-Type': 'application/json', 'X-Auth-Token': authToken},
                body: JSON.stringify({text})
            });
            closeModal('noteModal');
            document.getElementById('noteContent').value = '';
            fetchItems();
        }

        async function submitUrl() {
            const url = document.getElementById('urlContent').value;
            if (!url) return;
            await fetch('/api/items/url', {
                method: 'POST',
                headers: {'Content-Type': 'application/json', 'X-Auth-Token': authToken},
                body: JSON.stringify({url})
            });
            closeModal('urlModal');
            document.getElementById('urlContent').value = '';
            fetchItems();
        }

        // Drag and drop setup
        const dropzone = document.getElementById('dropzone');
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            dropzone.addEventListener(eventName, e => { e.preventDefault(); e.stopPropagation(); });
        });
        ['dragenter', 'dragover'].forEach(eventName => {
            dropzone.addEventListener(eventName, () => dropzone.classList.add('dragover'));
        });
        ['dragleave', 'drop'].forEach(eventName => {
            dropzone.addEventListener(eventName, () => dropzone.classList.remove('dragover'));
        });
        dropzone.addEventListener('drop', e => handleFileUpload(e.dataTransfer.files));

        function formatBytes(bytes) {
            if (!bytes || bytes === 0) return '0 B';
            const k = 1024, sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
        }

        function formatDate(timestamp) {
            return new Date(timestamp).toLocaleString();
        }

        function escapeHtml(str) {
            return (str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }
    </script>
</body>
</html>
    """.trimIndent()
}
