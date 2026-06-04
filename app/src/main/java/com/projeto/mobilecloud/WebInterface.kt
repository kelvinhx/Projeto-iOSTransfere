package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <title>Nexus Explorer</title>
            <style>
                :root { --bg: #090909; --card: #161618; --blue: #0A84FF; --accent: #30D158; }
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: white; margin: 0; }
                .toolbar { sticky: top; background: rgba(22,22,24,0.8); backdrop-filter: blur(10px); padding: 15px; display: flex; gap: 10px; align-items: center; }
                .breadcrumb { padding: 10px 15px; font-size: 14px; color: var(--blue); background: var(--card); overflow-x: auto; white-space: nowrap; }
                .file-list { padding: 10px; }
                .item { background: var(--card); margin-bottom: 8px; border-radius: 12px; padding: 15px; display: flex; align-items: center; justify-content: space-between; }
                .info { display: flex; align-items: center; gap: 12px; flex: 1; cursor: pointer; }
                .icon { font-size: 24px; }
                .actions { display: flex; gap: 15px; }
                .btn { background: var(--blue); border: none; color: white; padding: 10px 18px; border-radius: 10px; font-weight: bold; }
                #loader { width: 0%; height: 3px; background: var(--accent); position: fixed; top: 0; transition: 0.3s; }
            </style>
        </head>
        <body>
            <div id="loader"></div>
            <div class="toolbar">
                <button class="btn" onclick="goBack()">⬅️ Voltar</button>
                <label class="btn" style="background:var(--accent)">📤 Enviar <input type="file" id="up" hidden onchange="upload()"></label>
                <button class="btn" style="background:#444" onclick="mkdir()">➕ Pasta</button>
            </div>
            <div id="bc" class="breadcrumb">Armazenamento Interno</div>
            <div id="list" class="file-list"></div>

            <script>
                let currentPath = "";

                async function load(path = currentPath) {
                    currentPath = path;
                    document.getElementById('bc').innerText = "📍 /" + currentPath;
                    const res = await fetch('/api/list?path=' + encodeURIComponent(currentPath));
                    const files = await res.json();
                    let html = '';
                    files.forEach(f => {
                        html += `
                        <div class="item">
                            <div class="info" onclick="${"$"}{f.isDir ? "load('"+f.path+"')" : "fileAction('"+f.name+"')" }">
                                <span class="icon">${"$"}{f.isDir ? '📂' : '📄'}</span>
                                <div><b>${"$"}{f.name}</b><br><small style="color:#888">${"$"}{f.size}</small></div>
                            </div>
                            <div class="actions">
                                <span onclick="rename('${"$"}{f.name}')">✏️</span>
                                <span onclick="del('${"$"}{f.name}')">🗑️</span>
                            </div>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = html;
                }

                function goBack() {
                    if(!currentPath) return;
                    let parts = currentPath.split('/');
                    parts.pop();
                    load(parts.join('/'));
                }

                async function upload() {
                    const file = document.getElementById('up').files[0];
                    const fd = new FormData();
                    fd.append('file', file);
                    const xhr = new XMLHttpRequest();
                    xhr.open('POST', '/upload?path=' + encodeURIComponent(currentPath));
                    xhr.upload.onprogress = e => document.getElementById('loader').style.width = (e.loaded/e.total*100) + '%';
                    xhr.onload = () => { document.getElementById('loader').style.width = '0%'; load(); };
                    xhr.send(fd);
                }

                async function del(name) {
                    if(confirm('Apagar ' + name + '?')) {
                        await fetch('/api/action', {
                            method: 'POST',
                            body: new URLSearchParams({action: 'delete', source: currentPath + '/' + name})
                        });
                        load();
                    }
                }

                async function mkdir() {
                    const n = prompt("Nome da pasta:");
                    if(n) {
                        await fetch('/api/action', {
                            method: 'POST',
                            body: new URLSearchParams({action: 'mkdir', dest: currentPath + '/' + n})
                        });
                        load();
                    }
                }

                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}