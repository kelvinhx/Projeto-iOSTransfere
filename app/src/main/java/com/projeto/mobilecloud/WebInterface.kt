package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <title>Nexus Cloud Explorer</title>
            <style>
                :root { --blue: #007AFF; --bg: #000; --card: #1C1C1E; --green: #32D74B; }
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: white; margin: 0; padding-bottom: 80px; }
                .toolbar { background: #121212; padding: 15px; position: sticky; top: 0; z-index: 100; display: flex; gap: 10px; border-bottom: 1px solid #333; }
                .item { background: var(--card); padding: 15px; border-radius: 12px; margin: 10px; display: flex; align-items: center; justify-content: space-between; }
                .item-info { display: flex; align-items: center; gap: 15px; flex: 1; overflow: hidden; }
                .name { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: 500; }
                .btn { background: var(--blue); border: none; color: white; padding: 10px 15px; border-radius: 8px; font-weight: bold; font-size: 13px; }
                .actions { display: flex; gap: 10px; margin-left: 10px; }
                #clipboard-bar { position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); background: var(--blue); padding: 15px 30px; border-radius: 30px; display: none; box-shadow: 0 5px 20px rgba(0,0,0,0.5); z-index: 1000; }
            </style>
        </head>
        <body>
            <div class="toolbar">
                <button class="btn" onclick="goBack()">⬅️ Voltar</button>
                <label class="btn" style="background:var(--green)">📤 Enviar <input type="file" id="up" hidden onchange="upload()"></label>
                <button class="btn" style="background:#444" onclick="mkdir()">📁 +Pasta</button>
            </div>
            <div id="bc" style="padding:10px; font-size:12px; color:var(--blue)">/Início</div>
            <div id="list"></div>

            <div id="clipboard-bar">
                <span id="clip-msg"></span>
                <button class="btn" style="background:white; color:black; margin-left:15px" onclick="paste()">📋 Colar Aqui</button>
            </div>

            <script>
                let currentPath = "";
                let clipboard = { action: null, path: null, name: null };

                async function load(path = "") {
                    currentPath = path;
                    document.getElementById('bc').innerText = "📍 " + (path || "/Armazenamento Interno");
                    const r = await fetch('/api/list?path=' + encodeURIComponent(path));
                    const files = await r.json();
                    let html = '';
                    files.forEach(f => {
                        html += `
                        <div class="item">
                            <div class="item-info" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                                <span>${"$"}{f.isDir ? '📂' : '📄'}</span>
                                <div class="name">${"$"}{f.name}</div>
                            </div>
                            <div class="actions">
                                <span onclick="rename('${"$"}{f.relPath}', '${"$"}{f.name}')">✏️</span>
                                <span onclick="copyMove('${"$"}{f.relPath}', '${"$"}{f.name}', 'move')">✂️</span>
                                <span onclick="del('${"$"}{f.relPath}')">🗑️</span>
                            </div>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = html || '<p style="text-align:center;color:#444">Pasta Vazia</p>';
                }

                function copyMove(path, name, action) {
                    clipboard = { action, path, name };
                    document.getElementById('clip-msg').innerText = (action === 'move' ? 'Mover: ' : 'Copiar: ') + name;
                    document.getElementById('clipboard-bar').style.display = 'block';
                }

                async function paste() {
                    const p = new URLSearchParams();
                    p.append('action', clipboard.action);
                    p.append('path', clipboard.path);
                    p.append('dest', currentPath);
                    await fetch('/api/action', { method: 'POST', body: p });
                    document.getElementById('clipboard-bar').style.display = 'none';
                    load(currentPath);
                }

                async function rename(path, oldName) {
                    const n = prompt("Novo nome:", oldName);
                    if(n && n !== oldName) {
                        const p = new URLSearchParams(); p.append('action', 'rename'); p.append('path', path); p.append('dest', path.replace(oldName, n));
                        await fetch('/api/action', { method: 'POST', body: p });
                        load(currentPath);
                    }
                }

                async function del(path) { if(confirm('Apagar?')) { 
                    const p = new URLSearchParams(); p.append('action', 'delete'); p.append('path', path);
                    await fetch('/api/action', { method: 'POST', body: p }); load(currentPath); 
                } }

                async function mkdir() {
                    const n = prompt("Nome da pasta:");
                    if(n) {
                        const p = new URLSearchParams(); p.append('action', 'mkdir'); p.append('path', currentPath); p.append('name', n);
                        await fetch('/api/action', { method: 'POST', body: p }); load(currentPath);
                    }
                }

                function goBack() { 
                    if (!currentPath) return; 
                    let p = currentPath.split('/').filter(x => x); p.pop(); load(p.join('/')); 
                }

                async function upload() {
                    const fd = new FormData(); fd.append('file', document.getElementById('up').files[0]);
                    await fetch('/upload?path=' + encodeURIComponent(currentPath), { method: 'POST', body: fd });
                    load(currentPath);
                }

                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}