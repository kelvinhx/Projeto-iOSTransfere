package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html lang="pt-br">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>Nexus File Manager</title>
            <style>
                :root { --blue: #007AFF; --bg: #000; --card: #1C1C1E; }
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: white; margin: 0; }
                .toolbar { background: #121212; padding: 15px; position: sticky; top: 0; z-index: 100; border-bottom: 1px solid #333; display: flex; gap: 10px; }
                .breadcrumb { padding: 10px 15px; background: #1C1C1E; color: var(--blue); font-size: 14px; overflow-x: auto; white-space: nowrap; border-bottom: 1px solid #333; }
                .list { padding: 10px; }
                .item { background: var(--card); padding: 15px; border-radius: 12px; margin-bottom: 8px; display: flex; align-items: center; justify-content: space-between; }
                .item-info { display: flex; align-items: center; gap: 15px; flex: 1; }
                .icon { font-size: 24px; }
                .name { font-size: 16px; font-weight: 500; }
                .btn { background: var(--blue); border: none; color: white; padding: 10px 15px; border-radius: 8px; font-weight: bold; }
                .btn-danger { background: #FF453A; font-size: 12px; padding: 5px 10px; }
                #progress { width: 0%; height: 3px; background: var(--blue); position: fixed; top: 0; left: 0; transition: 0.3s; }
            </style>
        </head>
        <body>
            <div id="progress"></div>
            <div class="toolbar">
                <button class="btn" onclick="goBack()">⬅️ Voltar</button>
                <label class="btn">📤 Enviar <input type="file" id="up" hidden onchange="upload()"></label>
            </div>
            <div id="bc" class="breadcrumb">Armazenamento Interno</div>
            <div id="list" class="list"></div>

            <script>
                let currentPath = "";

                async function load(path = "") {
                    currentPath = path;
                    document.getElementById('bc').innerText = "📍 " + (path || "/Início");
                    const r = await fetch('/api/list?path=' + encodeURIComponent(path));
                    const files = await r.json();
                    let html = '';
                    files.forEach(f => {
                        html += `
                        <div class="item">
                            <div class="item-info" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                                <span class="icon">${"$"}{f.isDir ? '📂' : '📄'}</span>
                                <div>
                                    <div class="name">${"$"}{f.name}</div>
                                    <small style="color:#888">${"$"}{f.size}</small>
                                </div>
                            </div>
                            <button class="btn btn-danger" onclick="deleteFile('${"$"}{f.relPath}')">🗑️</button>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = html || '<p style="text-align:center;color:#666">Pasta Vazia</p>';
                }

                function goBack() {
                    if (!currentPath) return;
                    let parts = currentPath.split('/').filter(p => p);
                    parts.pop();
                    load(parts.join('/'));
                }

                async function upload() {
                    const f = document.getElementById('up').files[0];
                    const fd = new FormData(); fd.append('file', f);
                    const xhr = new XMLHttpRequest();
                    xhr.open('POST', '/upload?path=' + encodeURIComponent(currentPath));
                    xhr.upload.onprogress = e => document.getElementById('progress').style.width = (e.loaded/e.total*100) + '%';
                    xhr.onload = () => { document.getElementById('progress').style.width = '0'; load(currentPath); };
                    xhr.send(fd);
                }

                async function deleteFile(path) {
                    if (confirm('Apagar arquivo?')) {
                        const p = new URLSearchParams(); p.append('action', 'delete'); p.append('path', path);
                        await fetch('/api/action', { method: 'POST', body: p });
                        load(currentPath);
                    }
                }

                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}