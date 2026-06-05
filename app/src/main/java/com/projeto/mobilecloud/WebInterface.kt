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
                :root { --blue: #007AFF; --green: #34C759; --bg: #000; --card: #1C1C1E; --red: #FF453A; }
                body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: var(--bg); color: #fff; margin: 0; padding-bottom: 120px; }
                
                /* Header e Busca */
                .header { background: #121212; padding: 15px; position: sticky; top: 0; display: flex; gap: 10px; border-bottom: 1px solid #333; z-index: 100; align-items: center; }
                .search-bar { flex: 1; padding: 10px; border-radius: 8px; border: none; background: #2C2C2E; color: #fff; font-size: 14px; }
                
                /* Telemetria e Storage */
                .telemetry-bar { background: #1C1C1E; padding: 5px 15px; font-size: 10px; color: #5856D6; border-bottom: 1px solid #333; display: flex; justify-content: space-between; }
                .storage-bar { padding: 8px 15px; font-size: 11px; color: #8E8E93; background: #000; border-bottom: 1px solid #222; }
                
                /* Lista de Arquivos */
                .item { background: var(--card); padding: 15px; border-radius: 12px; margin: 10px; display: flex; align-items: center; justify-content: space-between; animation: fadeIn 0.3s ease; }
                .file-info { flex: 1; overflow: hidden; margin-right: 10px; }
                .file-name { font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; }
                .file-meta { font-size: 11px; color: #8E8E93; }
                
                /* Botões */
                .btn { background: var(--blue); color: white; border: none; padding: 10px 15px; border-radius: 8px; font-weight: bold; font-size: 14px; }
                .action-icon { font-size: 20px; cursor: pointer; padding: 5px; }

                /* Clipboard e Modais */
                #clip-bar { position: fixed; bottom: 0; left: 0; right: 0; background: var(--blue); padding: 20px; display: none; text-align: center; box-shadow: 0 -5px 15px rgba(0,0,0,0.5); z-index: 1000; }
                #preview-modal { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: #000; display: none; z-index: 2000; flex-direction: column; align-items: center; justify-content: center; }
                #preview-modal video, #preview-modal img { max-width: 95%; max-height: 80%; border-radius: 10px; }

                @keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }
            </style>
        </head>
        <body>
            <div id="preview-modal">
                <button class="btn" style="position:absolute; top:20px; right:20px;" onclick="closePreview()">✖ FECHAR</button>
                <div id="preview-content" style="width:100%; text-align:center"></div>
            </div>

            <div class="header">
                <button class="btn" onclick="goBack()">⬅</button>
                <input type="text" id="search" class="search-bar" placeholder="Buscar na TV..." oninput="filterFiles()">
                <label class="btn" style="background:var(--green)">📤 <input type="file" id="up" hidden onchange="upload()"></label>
            </div>

            <div class="telemetry-bar">
                <span>📡 NEXUS ENGINE ONLINE</span>
                <span id="ram-status">RAM: Verificando...</span>
            </div>

            <div id="storage" class="storage-bar">📊 Armazenamento: Carregando...</div>
            
            <div id="bc" style="padding:12px; font-size:12px; color:var(--blue); font-weight:bold;">📍 /Armazenamento</div>
            
            <div id="list"></div>

            <div id="clip-bar">
                <span id="clip-txt" style="font-size:12px; display:block; margin-bottom:10px"></span>
                <button class="btn" style="background:white; color:black;" onclick="paste()">📋 COLAR NESTA PASTA</button>
                <button class="btn" style="background:rgba(0,0,0,0.2); margin-left:10px;" onclick="clearClip()">✖</button>
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
                        updateStorage();
                        updateTelemetry();
                    } catch(e) { document.getElementById('list').innerHTML = "<p style='text-align:center'>Erro de conexão com a TV.</p>"; }
                }

                function render(files) {
                    let h = '';
                    files.forEach(f => {
                        h += `
                        <div class="item">
                            <div class="file-info" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : "preview('"+f.relPath+"', '"+f.name+"')" }">
                                <span class="file-name">${"$"}{f.icon} ${"$"}{f.name}</span>
                                <span class="file-meta">${"$"}{f.size}</span>
                            </div>
                            <div style="display:flex; gap:10px">
                                ${"$"}{!f.isDir ? `<span class="action-icon" onclick="openTV('${"$"}{f.relPath}')">▶️</span>` : ''}
                                <span class="action-icon" onclick="setClip('${"$"}{f.relPath}','${"$"}{f.name}')">✂️</span>
                                <span class="action-icon" onclick="del('${"$"}{f.relPath}')">🗑️</span>
                            </div>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = h || '<p style="text-align:center; color:#666; margin-top:50px">Pasta Vazia</p>';
                }

                function filterFiles() {
                    const q = document.getElementById('search').value.toLowerCase();
                    render(allFiles.filter(f => f.name.toLowerCase().includes(q)));
                }

                async function updateStorage() {
                    const s = await fetch('/api/storage');
                    const info = await s.json();
                    document.getElementById('storage').innerText = "📊 Livre: " + info.free + " / Total: " + info.total;
                }

                async function updateTelemetry() {
                    const r = await fetch('/api/logs');
                    const txt = await r.text();
                    document.getElementById('ram-status').innerText = txt.split('\n')[0];
                }

                function preview(path, name) {
                    const modal = document.getElementById('preview-modal');
                    const content = document.getElementById('preview-content');
                    const ext = name.split('.').pop().toLowerCase();
                    const url = '/api/stream?path=' + encodeURIComponent(path);
                    
                    if(['mp4','m4v','webm','mov'].includes(ext)) {
                        content.innerHTML = `<video controls autoplay src="${"$"}{url}"></video>`;
                    } else if(['jpg','jpeg','png','webp','gif'].includes(ext)) {
                        content.innerHTML = `<img src="${"$"}{url}">`;
                    } else { return; }
                    modal.style.display = "flex";
                }

                function closePreview() { document.getElementById('preview-modal').style.display = "none"; document.getElementById('preview-content').innerHTML = ""; }
                function setClip(p, n) { clip = { path: p, name: n }; document.getElementById('clip-bar').style.display="block"; document.getElementById('clip-txt').innerText="Mover: "+n; }
                function clearClip() { document.getElementById('clip-bar').style.display="none"; }

                async function paste() {
                    const p = new URLSearchParams({ action: 'move', path: clip.path, dest: curPath + '/' + clip.name });
                    await fetch('/api/action', { method: 'POST', body: p });
                    clearClip(); load(curPath);
                }

                async function openTV(p) { await fetch('/api/open?path=' + encodeURIComponent(p)); }
                async function del(p) { if(confirm('Apagar permanentemente?')) { await fetch('/api/action', { method: 'POST', body: new URLSearchParams({action:'delete', path:p}) }); load(curPath); } }
                function goBack() { let p = curPath.split('/').filter(x=>x); p.pop(); load(p.join('/')); }
                
                async function upload() {
                    const file = document.getElementById('up').files[0];
                    if(!file) return;
                    const fd = new FormData(); fd.append('file', file);
                    document.getElementById('storage').innerText = "📤 Enviando: " + file.name + "...";
                    await fetch('/upload?path='+encodeURIComponent(curPath), {method:'POST', body:fd});
                    load(curPath);
                }

                load();
                setInterval(updateTelemetry, 15000);
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}