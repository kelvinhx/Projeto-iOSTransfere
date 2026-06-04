package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nexus Explorer</title>
            <style>
                :root { --blue: #007AFF; --bg: #000; --card: #1C1C1E; }
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: white; margin: 0; padding-bottom: 100px; }
                .header { background: #121212; padding: 15px; position: sticky; top: 0; z-index: 100; display: flex; gap: 10px; border-bottom: 1px solid #333; }
                .item { background: var(--card); padding: 15px; border-radius: 12px; margin: 10px; display: flex; align-items: center; justify-content: space-between; }
                .btn { background: var(--blue); border: none; color: white; padding: 10px 15px; border-radius: 8px; font-weight: bold; }
                .action-bar { position: fixed; bottom: 0; left: 0; right: 0; background: #121212; padding: 20px; display: none; border-top: 1px solid #333; }
            </style>
        </head>
        <body>
            <div class="header">
                <button class="btn" onclick="goBack()">⬅️</button>
                <label class="btn" style="background:#32D74B">📤 Enviar <input type="file" id="up" hidden onchange="upload()"></label>
                <button class="btn" style="background:#444" onclick="mkdir()">📁 +Pasta</button>
            </div>
            <div id="bc" style="padding:10px; font-size:12px; color:var(--blue)">/Armazenamento</div>
            <div id="list"></div>
            
            <div id="action-bar" class="action-bar">
                <span id="clip-name"></span>
                <button class="btn" onclick="paste()">📋 Colar Aqui</button>
            </div>

            <script>
                let currentPath = "";
                let clipboard = { action: '', path: '', name: '' };

                async function load(path = "") {
                    currentPath = path;
                    document.getElementById('bc').innerText = "📍 " + (path || "/Início");
                    const r = await fetch('/api/list?path=' + encodeURIComponent(path));
                    const files = await r.json();
                    let html = '';
                    files.forEach(f => {
                        html += `
                        <div class="item">
                            <div style="flex:1" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                                ${"$"}{f.isDir ? '📂' : '📄'} ${"$"}{f.name}
                            </div>
                            <div style="display:flex; gap:15px">
                                <span onclick="rename('${"$"}{f.relPath}','${"$"}{f.name}')">✏️</span>
                                <span onclick="copyMove('${"$"}{f.relPath}','${"$"}{f.name}','move')">✂️</span>
                                <span onclick="del('${"$"}{f.relPath}')">🗑️</span>
                            </div>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = html;
                }

                function copyMove(path, name, action) {
                    clipboard = { action, path, name };
                    document.getElementById('action-bar').style.display = 'block';
                    document.getElementById('clip-name').innerText = "Mover: " + name;
                }

                async function paste() {
                    const p = new URLSearchParams({ action: clipboard.action, path: clipboard.path, dest: currentPath });
                    await fetch('/api/action', { method: 'POST', body: p });
                    document.getElementById('action-bar').style.display = 'none';
                    load(currentPath);
                }

                async function rename(path, old) {
                    const n = prompt("Novo nome:", old);
                    if(n) {
                        const p = new URLSearchParams({ action: 'rename', path: path, dest: path.replace(old, n) });
                        await fetch('/api/action', { method: 'POST', body: p });
                        load(currentPath);
                    }
                }

                async function del(p) { if(confirm('Apagar?')) { 
                    await fetch('/api/action', { method: 'POST', body: new URLSearchParams({action:'delete', path:p}) });
                    load(currentPath);
                } }

                async function mkdir() {
                    const n = prompt("Nome da pasta:");
                    if(n) {
                        await fetch('/api/action', { method: 'POST', body: new URLSearchParams({action:'mkdir', path:currentPath, name:n}) });
                        load(currentPath);
                    }
                }

                function goBack() { let p = currentPath.split('/').filter(x=>x); p.pop(); load(p.join('/')); }
                async function upload() {
                    const fd = new FormData(); fd.append('file', document.getElementById('up').files[0]);
                    await fetch('/upload?path='+encodeURIComponent(currentPath), {method:'POST', body:fd});
                    load(currentPath);
                }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}