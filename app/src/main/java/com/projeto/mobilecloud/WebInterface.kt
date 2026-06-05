package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html lang="pt-br">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>Nexus Explorer Pro</title>
            <style>
                :root { --blue: #007AFF; --green: #34C759; --bg: #000; --card: #1C1C1E; }
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: #fff; margin: 0; padding-bottom: 120px; }
                .header { background: #121212; padding: 15px; position: sticky; top: 0; display: flex; gap: 10px; border-bottom: 1px solid #333; z-index: 100; align-items: center; }
                .search-bar { flex: 1; padding: 10px; border-radius: 8px; border: none; background: #2C2C2E; color: #fff; font-size: 14px; }
                .item { background: var(--card); padding: 15px; border-radius: 12px; margin: 10px; display: flex; align-items: center; justify-content: space-between; }
                .btn { background: var(--blue); color: white; border: none; padding: 10px 15px; border-radius: 8px; font-weight: bold; }
                .action-icon { font-size: 20px; cursor: pointer; padding: 5px; }
                #preview-modal { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: #000; display: none; z-index: 2000; flex-direction: column; align-items: center; justify-content: center; }
            </style>
        </head>
        <body>
            <div id="preview-modal">
                <button class="btn" style="position:absolute; top:20px; right:20px;" onclick="closePreview()">✖</button>
                <div id="preview-content" style="width:100%; text-align:center"></div>
            </div>

            <div class="header">
                <button class="btn" onclick="goBack()">⬅</button>
                <input type="text" id="search" class="search-bar" placeholder="Buscar na TV..." oninput="filterFiles()">
                <label class="btn" style="background:var(--green)">📤 <input type="file" id="up" hidden onchange="upload()"></label>
            </div>

            <div id="bc" style="padding:15px; font-size:12px; color:var(--blue); font-weight:bold;">📍 /Armazenamento</div>
            <div id="list"></div>

            <script>
                let curPath = "";
                let allFiles = [];

                async function load(p = "") {
                    curPath = p;
                    document.getElementById('bc').innerText = "📍 " + (p || "/Início");
                    try {
                        const r = await fetch('/api/list?path=' + encodeURIComponent(p));
                        allFiles = await r.json();
                        render(allFiles);
                    } catch(e) { document.getElementById('list').innerHTML = "Conexão perdida."; }
                }

                function render(files) {
                    let h = '';
                    files.forEach(f => {
                        h += `
                        <div class="item">
                            <div style="flex:1; overflow:hidden" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : "preview('"+f.relPath+"', '"+f.name+"')" }">
                                <span style="display:block; white-space:nowrap; overflow:hidden; text-overflow:ellipsis">${"$"}{f.icon} ${"$"}{f.name}</span>
                                <small style="color:#8E8E93">${"$"}{f.size}</small>
                            </div>
                            <div style="display:flex; gap:8px">
                                ${"$"}{!f.isDir ? `<a href="/api/download?path=${"$"}{encodeURIComponent(f.relPath)}" style="text-decoration:none">📥</a>` : ''}
                                ${"$"}{!f.isDir ? `<span class="action-icon" onclick="openTV('${"$"}{f.relPath}')">▶️</span>` : ''}
                                <span class="action-icon" onclick="del('${"$"}{f.relPath}')">🗑️</span>
                            </div>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = h || '<p style="text-align:center">Vazio</p>';
                }

                function filterFiles() {
                    const q = document.getElementById('search').value.toLowerCase();
                    render(allFiles.filter(f => f.name.toLowerCase().includes(q)));
                }

                function preview(path, name) {
                    const modal = document.getElementById('preview-modal');
                    const content = document.getElementById('preview-content');
                    const ext = name.split('.').pop().toLowerCase();
                    const url = '/api/stream?path=' + encodeURIComponent(path);
                    if(['mp4','m4v','webm','mov','mp3'].includes(ext)) {
                        content.innerHTML = `<video controls autoplay style="max-width:90%" src="${"$"}{url}"></video>`;
                    } else if(['jpg','jpeg','png','webp','gif'].includes(ext)) {
                        content.innerHTML = `<img style="max-width:90%" src="${"$"}{url}">`;
                    } else { return; }
                    modal.style.display = "flex";
                }

                function closePreview() { document.getElementById('preview-modal').style.display = "none"; document.getElementById('preview-content').innerHTML = ""; }
                async function openTV(p) { await fetch('/api/open?path=' + encodeURIComponent(p)); }
                async function del(p) { if(confirm('Apagar?')) { await fetch('/api/action', { method: 'POST', body: new URLSearchParams({action:'delete', path:p}) }); load(curPath); } }
                function goBack() { let p = curPath.split('/').filter(x=>x); p.pop(); load(p.join('/')); }
                async function upload() {
                    const fd = new FormData(); fd.append('file', document.getElementById('up').files[0]);
                    await fetch('/upload?path='+encodeURIComponent(curPath), {method:'POST', body:fd}); load(curPath);
                }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}