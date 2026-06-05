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
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: #fff; margin: 0; padding-bottom: 100px; }
                .header { background: #121212; padding: 15px; position: sticky; top: 0; display: flex; gap: 10px; border-bottom: 1px solid #333; z-index: 100; align-items: center; }
                .item { background: var(--card); padding: 15px; border-radius: 12px; margin: 10px; display: flex; align-items: center; justify-content: space-between; }
                .btn { background: var(--blue); color: white; border: none; padding: 10px 15px; border-radius: 8px; font-weight: bold; }
                /* Modal de Visualização */
                #preview-modal { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: #000; display: none; z-index: 200; flex-direction: column; align-items: center; justify-content: center; }
                #preview-modal video, #preview-modal img { max-width: 95%; max-height: 80%; }
            </style>
        </head>
        <body>
            <div id="preview-modal">
                <button class="btn" style="position:absolute; top:20px; right:20px;" onclick="closePreview()">FECHAR</button>
                <div id="preview-content" style="width:100%; text-align:center"></div>
            </div>

            <div class="header">
                <button class="btn" onclick="goBack()">⬅️</button>
                <label class="btn" style="background:#32D74B">📤 <input type="file" id="up" hidden onchange="upload()"></label>
            </div>
            <div id="bc" style="padding:10px; font-size:12px; color:var(--blue)">📍 /Raiz</div>
            <div id="list"></div>

            <script>
                let curPath = "";

                async function load(p = "") {
                    curPath = p;
                    document.getElementById('bc').innerText = "📍 " + (p || "/Início");
                    const r = await fetch('/api/list?path=' + encodeURIComponent(p));
                    const files = await r.json();
                    let h = '';
                    files.forEach(f => {
                        h += `<div class="item">
                            <div style="flex:1" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : "preview('"+f.relPath+"', '"+f.name+"')" }">
                                ${"$"}{f.icon} ${"$"}{f.name} <br><small style="color:#666">${"$"}{f.size}</small>
                            </div>
                            <div style="display:flex; gap:10px">
                                ${"$"}{!f.isDir ? `<span onclick="openTV('${"$"}{f.relPath}')">▶️</span>` : ''}
                                <span onclick="del('${"$"}{f.relPath}')">🗑️</span>
                            </div>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = h;
                }

                function preview(path, name) {
                    const modal = document.getElementById('preview-modal');
                    const content = document.getElementById('preview-content');
                    const ext = name.split('.').pop().toLowerCase();
                    const url = '/api/stream?path=' + encodeURIComponent(path);
                    
                    if(['mp4','m4v','webm'].includes(ext)) {
                        content.innerHTML = `<video controls autoplay src="${"$"}{url}"></video>`;
                    } else if(['jpg','jpeg','png','webp','gif'].includes(ext)) {
                        content.innerHTML = `<img src="${"$"}{url}">`;
                    } else {
                        alert("Arquivo sem visualização rápida disponível."); return;
                    }
                    modal.style.display = "flex";
                }

                function closePreview() { 
                    document.getElementById('preview-modal').style.display = "none"; 
                    document.getElementById('preview-content').innerHTML = "";
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