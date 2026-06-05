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
                body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: var(--bg); color: #fff; margin: 0; padding-bottom: 100px; }
                .header { background: #121212; padding: 15px; position: sticky; top: 0; display: flex; gap: 10px; border-bottom: 1px solid #333; z-index: 100; align-items: center; }
                .search-bar { flex: 1; padding: 10px; border-radius: 8px; border: none; background: #2C2C2E; color: #fff; font-size: 14px; }
                .storage-bar { padding: 8px 15px; font-size: 11px; color: #8E8E93; background: #121212; border-bottom: 1px solid #333; }
                .item { background: var(--card); padding: 15px; border-radius: 12px; margin: 10px; display: flex; align-items: center; justify-content: space-between; }
                .btn { background: var(--blue); color: white; border: none; padding: 10px 15px; border-radius: 8px; font-weight: bold; }
                #clip-bar { position: fixed; bottom: 0; left: 0; right: 0; background: var(--blue); padding: 20px; display: none; text-align: center; box-shadow: 0 -5px 15px rgba(0,0,0,0.5); }
            </style>
        </head>
        <body>
            <div class="header">
                <button class="btn" onclick="goBack()">⬅️</button>
                <input type="text" id="search" class="search-bar" placeholder="Buscar na TV..." oninput="filterFiles()">
                <label class="btn" style="background:var(--green)">📤 <input type="file" id="up" hidden onchange="upload()"></label>
            </div>
            <div id="storage" class="storage-bar">Calculando espaço...</div>
            <div id="bc" style="padding:10px; font-size:12px; color:var(--blue)">📍 /Armazenamento</div>
            <div id="list"></div>

            <div id="clip-bar">
                <span id="clip-txt"></span>
                <button class="btn" style="background:white; color:black; margin-left:10px" onclick="paste()">📋 COLAR AQUI</button>
            </div>

            <script>
                let curPath = "";
                let allFiles = [];
                let clip = { path: "", name: "" };

                async function load(p = "") {
                    curPath = p;
                    document.getElementById('bc').innerText = "📍 " + (p || "/Início");
                    try {
                        const r = await fetch('/api/list?path=' + encodeURIComponent(p));
                        allFiles = await r.json();
                        render(allFiles);
                        
                        const s = await fetch('/api/storage');
                        const info = await s.json();
                        document.getElementById('storage').innerText = "Livre: " + info.free + " / Total: " + info.total;
                    } catch(e) { document.getElementById('list').innerHTML = "Erro ao conectar."; }
                }

                function render(files) {
                    let h = '';
                    files.forEach(f => {
                        h += `<div class="item">
                            <div style="flex:1" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                                ${"$"}{f.isDir ? '📁' : '📄'} ${"$"}{f.name} <br>
                                <small style="color:#666">${"$"}{f.size}</small>
                            </div>
                            <div style="display:flex; gap:12px; font-size:18px">
                                ${"$"}{!f.isDir ? `<span onclick="openTV('${"$"}{f.relPath}')">▶️</span>` : ''}
                                <span onclick="setClip('${"$"}{f.relPath}','${"$"}{f.name}')">✂️</span>
                                <span onclick="del('${"$"}{f.relPath}')">🗑️</span>
                            </div>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = h || '<p style="text-align:center;color:#666">Pasta Vazia</p>';
                }

                function filterFiles() {
                    const q = document.getElementById('search').value.toLowerCase();
                    render(allFiles.filter(f => f.name.toLowerCase().includes(q)));
                }

                function setClip(p, n) { clip = { path: p, name: n }; document.getElementById('clip-bar').style.display="block"; document.getElementById('clip-txt').innerText="Mover: "+n; }
                async function paste() {
                    const p = new URLSearchParams({ action: 'move', path: clip.path, dest: curPath + '/' + clip.name });
                    await fetch('/api/action', { method: 'POST', body: p });
                    document.getElementById('clip-bar').style.display="none"; load(curPath);
                }

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