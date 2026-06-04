package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${AppConfig.APP_NAME}</title>
            <style>
                body { font-family: -apple-system, sans-serif; background: ${AppConfig.THEME_BG}; color: white; margin: 0; padding-bottom: 80px; }
                .header { background: #121212; padding: 15px; position: sticky; top: 0; display: flex; gap: 10px; border-bottom: 1px solid #333; }
                .item { background: #1C1C1E; padding: 15px; border-radius: 12px; margin: 10px; display: flex; align-items: center; justify-content: space-between; }
                .btn { background: ${AppConfig.THEME_ACCENT}; border: none; color: white; padding: 10px 15px; border-radius: 8px; font-weight: bold; }
                .log-link { font-size: 10px; color: #444; text-decoration: none; position: fixed; bottom: 5px; right: 5px; }
            </style>
        </head>
        <body>
            <div class="header">
                <button class="btn" onclick="goBack()">⬅️</button>
                <label class="btn" style="background:#32D74B">📤 Enviar <input type="file" id="up" hidden onchange="upload()"></label>
                <button class="btn" style="background:#444" onclick="mkdir()">📁 +Pasta</button>
            </div>
            <div id="bc" style="padding:10px; color:${AppConfig.THEME_ACCENT}">📍 /Início</div>
            <div id="list"></div>
            
            <a href="/logs" class="log-link">Ver System Logs</a>

            <script>
                let currentPath = "";
                async function load(path = "") {
                    currentPath = path;
                    document.getElementById('bc').innerText = "📍 " + (path || "/Armazenamento");
                    const r = await fetch('/api/list?path=' + encodeURIComponent(path));
                    const files = await r.json();
                    let html = '';
                    files.forEach(f => {
                        html += `
                        <div class="item">
                            <div style="flex:1" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                                ${"$"}{f.isDir ? '📂' : '📄'} ${"$"}{f.name} <br>
                                <small style="color:#666">${"$"}{f.size}</small>
                            </div>
                            <span onclick="del('${"$"}{f.relPath}')">🗑️</span>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = html || '<p style="text-align:center;color:#444">Vazio</p>';
                }
                function goBack() { let p = currentPath.split('/').filter(x=>x); p.pop(); load(p.join('/')); }
                async function upload() {
                    const fd = new FormData(); fd.append('file', document.getElementById('up').files[0]);
                    await fetch('/upload?path='+encodeURIComponent(currentPath), {method:'POST', body:fd});
                    load(currentPath);
                }
                async function del(p) { if(confirm('Apagar?')) { 
                    await fetch('/api/action', { method: 'POST', body: new URLSearchParams({action:'delete', path:p}) });
                    load(currentPath);
                } }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}