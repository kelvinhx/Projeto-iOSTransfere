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
                :root { --blue: #0A84FF; --green: #30D158; --red: #FF453A; --gray: #8E8E93; }
                * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #000; color: #fff; margin: 0; padding-bottom: 100px; overflow-x: hidden; }
                
                /* Header Estilo Vidro */
                .header { background: rgba(18, 18, 18, 0.8); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); position: sticky; top: 0; z-index: 1000; padding: 15px; border-bottom: 0.5px solid #333; }
                .nav-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
                .search-container { position: relative; width: 100%; }
                .search-bar { width: 100%; padding: 10px 15px; border-radius: 10px; border: none; background: #1C1C1E; color: #fff; font-size: 16px; outline: none; }
                
                /* Lista de Itens */
                .list-container { padding: 10px; }
                .item { background: #1C1C1E; border-radius: 14px; margin-bottom: 10px; display: flex; align-items: center; padding: 12px; transition: transform 0.1s; border: 0.5px solid #2C2C2E; }
                .item:active { transform: scale(0.98); background: #2C2C2E; }
                .file-icon { font-size: 28px; margin-right: 15px; display: flex; align-items: center; justify-content: center; width: 45px; height: 45px; background: rgba(255,255,255,0.05); border-radius: 10px; }
                .file-info { flex: 1; min-width: 0; }
                .file-name { font-size: 16px; font-weight: 500; display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
                .file-size { font-size: 12px; color: var(--gray); }
                
                /* Botões e Ações */
                .btn { background: var(--blue); color: #fff; border: none; padding: 10px 18px; border-radius: 10px; font-weight: 600; font-size: 14px; cursor: pointer; display: flex; align-items: center; gap: 8px; }
                .btn-icon { background: none; border: none; color: var(--blue); font-size: 20px; padding: 8px; }
                
                /* Modal de Visualização */
                #preview-modal { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.95); display: none; z-index: 2000; flex-direction: column; align-items: center; justify-content: center; }
                .close-btn { position: absolute; top: 40px; right: 20px; background: rgba(255,255,255,0.1); color: white; border: none; padding: 10px 15px; border-radius: 50%; font-size: 20px; }
                
                /* Barra de Status Inferior */
                .bottom-bar { position: fixed; bottom: 0; left: 0; right: 0; background: rgba(18, 18, 18, 0.9); backdrop-filter: blur(20px); padding: 15px; border-top: 0.5px solid #333; display: flex; justify-content: space-around; z-index: 1000; }
                
                /* Animações */
                @keyframes slideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }
                .animate-slide { animation: slideUp 0.3s ease-out; }
            </style>
        </head>
        <body>
            <div id="preview-modal">
                <button class="close-btn" onclick="closePreview()">✕</button>
                <div id="preview-content" style="width:100%; text-align:center"></div>
            </div>

            <div class="header">
                <div class="nav-row">
                    <button class="btn" style="background:#333; padding: 10px;" onclick="goBack()">⇠</button>
                    <div class="search-container">
                        <input type="text" id="search" class="search-bar" placeholder="Pesquisar arquivos..." oninput="filterFiles()">
                    </div>
                    <label class="btn" style="background:var(--green)">
                        ➕ <input type="file" id="up" hidden onchange="upload()">
                    </label>
                </div>
                <div id="bc" style="font-size:12px; color:var(--blue); font-weight:600; opacity:0.8">/Armazenamento</div>
            </div>

            <div id="storage-header" style="padding: 10px 15px; font-size: 11px; color: var(--gray); text-align: right;">📊 Calculando...</div>

            <div id="list" class="list-container"></div>

            <div class="bottom-bar">
                <div style="text-align:center" onclick="load('')">
                    <div style="font-size:20px">🏠</div>
                    <div style="font-size:10px; color:var(--gray)">Início</div>
                </div>
                <div style="text-align:center" onclick="alert('Funcionalidade em breve!')">
                    <div style="font-size:20px">⭐</div>
                    <div style="font-size:10px; color:var(--gray)">Favoritos</div>
                </div>
                <div style="text-align:center" onclick="location.reload()">
                    <div style="font-size:20px">🔄</div>
                    <div style="font-size:10px; color:var(--gray)">Recarregar</div>
                </div>
            </div>

            <script>
                let curPath = "";
                let allFiles = [];

                async function load(p = "") {
                    curPath = p;
                    document.getElementById('bc').innerText = "📍 " + (p || "/Raiz");
                    try {
                        const r = await fetch('/api/list?path=' + encodeURIComponent(p));
                        allFiles = await r.json();
                        render(allFiles);
                        updateStorage();
                    } catch(e) { document.getElementById('list').innerHTML = "<p style='text-align:center'>TV Offline</p>"; }
                }

                function render(files) {
                    let h = '';
                    files.forEach(f => {
                        h += `
                        <div class="item">
                            <div class="file-icon">${"$"}{f.icon}</div>
                            <div class="file-info" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : "preview('"+f.relPath+"', '"+f.name+"')" }">
                                <span class="file-name">${"$"}{f.name}</span>
                                <span class="file-size">${"$"}{f.size}</span>
                            </div>
                            <div style="display:flex">
                                ${"$"}{!f.isDir ? `<button class="btn-icon" onclick="openTV('${"$"}{f.relPath}')">▶️</button>` : ''}
                                ${"$"}{!f.isDir ? `<a href="/api/download?path=${"$"}{encodeURIComponent(f.relPath)}" class="btn-icon" style="text-decoration:none">📥</a>` : ''}
                                <button class="btn-icon" style="color:var(--red)" onclick="del('${"$"}{f.relPath}')">🗑️</button>
                            </div>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = h || '<p style='text-align:center; margin-top:40px; color:var(--gray)'>Nenhum arquivo encontrado</p>';
                }

                async function updateStorage() {
                    const s = await fetch('/api/storage');
                    const info = await s.json();
                    document.getElementById('storage-header').innerText = "LIVRE: " + info.free + " / TOTAL: " + info.total;
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
                        content.innerHTML = `<video controls autoplay style="max-width:100%; box-shadow: 0 10px 40px rgba(0,0,0,0.5)" src="${"$"}{url}"></video>`;
                    } else if(['jpg','jpeg','png','webp','gif'].includes(ext)) {
                        content.innerHTML = `<img style="max-width:100%; border-radius:10px;" src="${"$"}{url}">`;
                    } else { return; }
                    modal.style.display = "flex";
                }

                function closePreview() { document.getElementById('preview-modal').style.display = "none"; document.getElementById('preview-content').innerHTML = ""; }
                async function openTV(p) { await fetch('/api/open?path=' + encodeURIComponent(p)); }
                async function del(p) { if(confirm('Excluir este arquivo da TV?')) { await fetch('/api/action', { method: 'POST', body: new URLSearchParams({action:'delete', path:p}) }); load(curPath); } }
                function goBack() { let p = curPath.split('/').filter(x=>x); p.pop(); load(p.join('/')); }
                
                async function upload() {
                    const file = document.getElementById('up').files[0];
                    if(!file) return;
                    const fd = new FormData(); fd.append('file', file);
                    document.getElementById('storage-header').innerText = "🚀 Enviando: " + file.name;
                    await fetch('/upload?path='+encodeURIComponent(curPath), {method:'POST', body:fd});
                    load(curPath);
                }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}