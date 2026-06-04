package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nexus Cloud Explorer</title>
            <style>
                :root { --blue: #007AFF; --green: #32D74B; --bg: #000; }
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: white; margin: 0; padding-bottom: 100px; }
                .header { background: #121212; padding: 15px; position: sticky; top: 0; display: flex; gap: 10px; z-index: 100; border-bottom: 1px solid #333; }
                .item { background: #1C1C1E; padding: 15px; border-radius: 12px; margin: 10px; display: flex; align-items: center; justify-content: space-between; }
                .btn { background: var(--blue); border: none; color: white; padding: 10px 15px; border-radius: 8px; font-weight: bold; }
                .actions { display: flex; gap: 15px; font-size: 18px; }
                #clipboard-bar { position: fixed; bottom: 0; left: 0; right: 0; background: var(--blue); padding: 20px; display: none; text-align: center; }
            </style>
        </head>
        <body>
            <div class="header">
                <button class="btn" onclick="goBack()">⬅️</button>
                <label class="btn" style="background:var(--green)">📤 Enviar <input type="file" id="up" hidden onchange="upload()"></label>
                <button class="btn" style="background:#444" onclick="mkdir()">📁 +Pasta</button>
            </div>
            <div id="bc" style="padding:15px; color:var(--blue)">📍 /Raiz</div>
            <div id="list"></div>

            <div id="clipboard-bar">
                <span id="clip-label"></span>
                <button class="btn" style="background:white; color:black; margin-left:15px" onclick="paste()">📋 COLAR AQUI</button>
                <button class="btn" style="background:red; margin-left:5px" onclick="clearClip()">X</button>
            </div>

            <script>
                let currentPath = "";
                let clipboard = { action: '', source: '', name: '' };

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
                            <div class="actions">
                                <span onclick="rename('${"$"}{f.relPath}', '${"$"}{f.name}')">✏️</span>
                                <span onclick="setClip('move', '${"$"}{f.relPath}', '${"$"}{f.name}')">✂️</span>
                                <span onclick="del('${"$"}{f.relPath}')">🗑️</span>
                            </div>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = html;
                }

                function setClip(action, path, name) {
                    clipboard = { action, source: path, name };
                    document.getElementById('clipboard-bar').style.display = 'block';
                    document.getElementById('clip-label').innerText = "Recortado: " + name;
                }

                async function paste() {
                    const p = new URLSearchParams({ action: clipboard.action, path: clipboard.source, dest: currentPath + '/' + clipboard.name });
                    await fetch('/api/action', { method: 'POST', body: p });
                    clearClip(); load(currentPath);
                }

                function clearClip() { document.getElementById('clipboard-bar').style.display = 'none'; }

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