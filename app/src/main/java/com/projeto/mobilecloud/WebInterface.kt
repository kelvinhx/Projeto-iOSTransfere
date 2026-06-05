package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nexus Cloud Pro</title>
            <style>
                :root { --blue: #007AFF; --bg: #000; --card: #1C1C1E; }
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: #fff; margin: 0; padding-bottom: 50px; }
                .header { background: #121212; padding: 15px; position: sticky; top: 0; z-index: 100; border-bottom: 1px solid #333; }
                .search-bar { width: 90%; margin: 10px auto; display: block; padding: 12px; border-radius: 10px; border: none; background: #222; color: #fff; }
                .item { background: var(--card); padding: 15px; border-radius: 12px; margin: 10px; display: flex; align-items: center; justify-content: space-between; }
                .btn { background: var(--blue); color: white; border: none; padding: 10px 15px; border-radius: 8px; font-weight: bold; }
                .storage-info { font-size: 11px; text-align: center; color: #888; margin: 5px 0; }
            </style>
        </head>
        <body>
            <div class="header">
                <button class="btn" onclick="goBack()">⬅️</button>
                <input type="text" class="search-bar" id="search" placeholder="🔍 Buscar na TV..." oninput="filterFiles()">
                <label class="btn" style="background:#32D74B">📤 <input type="file" id="up" hidden onchange="upload()"></label>
            </div>
            <div id="storage" class="storage-info">Carregando memória...</div>
            <div id="bc" style="padding:10px; color:var(--blue); font-size:13px;">📍 /Início</div>
            <div id="list"></div>

            <script>
                let curPath = "";
                let allFiles = [];

                async function load(path = "") {
                    curPath = path;
                    document.getElementById('bc').innerText = "📍 " + (path || "/Início");
                    const r = await fetch('/api/list?path=' + encodeURIComponent(path));
                    allFiles = await r.json();
                    
                    // Atualiza info de storage
                    const s = await fetch('/api/storage');
                    const info = await s.json();
                    document.getElementById('storage').innerText = "Livre: " + info.free + " / Total: " + info.total;
                    
                    render(allFiles);
                }

                function render(data) {
                    let h = '';
                    data.forEach(f => {
                        h += `<div class="item">
                            <div style="flex:1" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                                ${"$"}{f.icon} ${"$"}{f.name} <br><small style="color:#666">${"$"}{f.size}</small>
                            </div>
                            <span onclick="del('${"$"}{f.relPath}')">🗑️</span>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = h || '<p style="text-align:center">Nenhum item</p>';
                }

                function filterFiles() {
                    const term = document.getElementById('search').value.toLowerCase();
                    const filtered = allFiles.filter(f => f.name.toLowerCase().includes(term));
                    render(filtered);
                }

                function goBack() { let p = curPath.split('/').filter(x=>x); p.pop(); load(p.join('/')); }
                async function upload() {
                    const fd = new FormData(); fd.append('file', document.getElementById('up').files[0]);
                    await fetch('/upload?path='+encodeURIComponent(curPath), {method:'POST', body:fd}); load(curPath);
                }
                async function del(p) { if(confirm('Apagar?')) { 
                    await fetch('/api/action', { method: 'POST', body: new URLSearchParams({action:'delete', path:p}) }); load(curPath); 
                } }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}