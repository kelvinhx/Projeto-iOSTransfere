package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <title>Nexus Explorer</title>
            <style>
                :root { --blue: #007AFF; --bg: #000; --card: #1C1C1E; }
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: white; margin: 0; }
                .header { background: #121212; padding: 15px; position: sticky; top: 0; z-index: 100; display: flex; justify-content: space-between; border-bottom: 1px solid #333; }
                .breadcrumb { padding: 12px; background: #1C1C1E; font-size: 14px; color: var(--blue); border-bottom: 1px solid #333; overflow-x: auto; }
                .item { background: var(--card); padding: 15px; border-radius: 12px; margin: 10px; display: flex; align-items: center; justify-content: space-between; }
                .item-main { display: flex; align-items: center; gap: 15px; flex: 1; }
                .icon { font-size: 24px; }
                .btn { background: var(--blue); border: none; color: white; padding: 10px 15px; border-radius: 8px; font-weight: bold; }
                #prog { width: 0%; height: 3px; background: var(--blue); position: fixed; top: 0; transition: 0.2s; }
            </style>
        </head>
        <body>
            <div id="prog"></div>
            <div class="header">
                <button class="btn" onclick="goBack()">⬅️ Voltar</button>
                <label class="btn" style="background:#32D74B">📤 Enviar <input type="file" id="f" hidden onchange="upload()"></label>
            </div>
            <div id="bc" class="breadcrumb">/Armazenamento</div>
            <div id="list"></div>

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
                            <div class="item-main" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                                <span class="icon">${"$"}{f.isDir ? '📂' : '📄'}</span>
                                <div><b>${"$"}{f.name}</b><br><small style="color:#888">${"$"}{f.size}</small></div>
                            </div>
                            <button style="background:none;border:none;font-size:20px;" onclick="del('${"$"}{f.relPath}')">🗑️</button>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = html || '<p style="text-align:center;color:#444">Pasta Vazia</p>';
                }

                function goBack() {
                    if (!currentPath) return;
                    let p = currentPath.split('/').filter(x => x); p.pop();
                    load(p.join('/'));
                }

                async function upload() {
                    const file = document.getElementById('f').files[0];
                    const fd = new FormData(); fd.append('file', file);
                    const xhr = new XMLHttpRequest();
                    xhr.open('POST', '/upload?path=' + encodeURIComponent(currentPath));
                    xhr.upload.onprogress = e => document.getElementById('prog').style.width = (e.loaded/e.total*100) + '%';
                    xhr.onload = () => { document.getElementById('prog').style.width = '0'; load(currentPath); };
                    xhr.send(fd);
                }

                async function del(path) {
                    if(confirm('Apagar item?')) {
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