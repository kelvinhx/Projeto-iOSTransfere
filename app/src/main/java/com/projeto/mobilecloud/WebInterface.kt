package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nexus Explorer Pro</title>
            <style>
                body { font-family: sans-serif; background: #000; color: #fff; margin: 0; padding-bottom: 100px; }
                .header { background: #121212; padding: 15px; position: sticky; top: 0; display: flex; gap: 10px; border-bottom: 1px solid #333; z-index: 100; }
                .item { background: #1C1C1E; padding: 15px; border-radius: 12px; margin: 10px; display: flex; align-items: center; justify-content: space-between; }
                .btn { background: #007AFF; color: white; border: none; padding: 12px; border-radius: 8px; font-weight: bold; }
                #clip-bar { position: fixed; bottom: 0; left: 0; right: 0; background: #007AFF; padding: 20px; display: none; text-align: center; }
            </style>
        </head>
        <body>
            <div class="header">
                <button class="btn" onclick="goBack()">⬅️</button>
                <label class="btn" style="background:#32D74B">📤 Enviar <input type="file" id="up" hidden onchange="upload()"></label>
                <button class="btn" style="background:#444" onclick="mkdir()">📁 +Pasta</button>
            </div>
            <div id="bc" style="padding:15px; color:#007AFF">📍 /Armazenamento</div>
            <div id="list"></div>

            <div id="clip-bar">
                <span id="clip-txt"></span>
                <button class="btn" style="background:white; color:black; margin-left:10px" onclick="paste()">📋 COLAR AQUI</button>
            </div>

            <script>
                let curPath = "";
                let clip = { path: "", name: "" };

                async function load(p = "") {
                    curPath = p;
                    document.getElementById('bc').innerText = "📍 " + (p || "/Início");
                    const r = await fetch('/api/list?path=' + encodeURIComponent(p));
                    const files = await r.json();
                    let h = '';
                    files.forEach(f => {
                        h += `<div class="item">
                            <div style="flex:1" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                                ${"$"}{f.isDir ? '📂' : '📄'} ${"$"}{f.name} <br>
                                <small style="color:#666">${"$"}{f.size}</small>
                            </div>
                            <div style="display:flex; gap:15px">
                                <span onclick="rename('${"$"}{f.relPath}','${"$"}{f.name}')">✏️</span>
                                <span onclick="setClip('${"$"}{f.relPath}','${"$"}{f.name}')">✂️</span>
                                <span onclick="del('${"$"}{f.relPath}')">🗑️</span>
                            </div>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = h;
                }

                function setClip(p, n) {
                    clip = { path: p, name: n };
                    document.getElementById('clip-txt').innerText = "Recortado: " + n;
                    document.getElementById('clip-bar').style.display = "block";
                }

                async function paste() {
                    const p = new URLSearchParams({ action: 'move', path: clip.path, dest: curPath + '/' + clip.name });
                    await fetch('/api/action', { method: 'POST', body: p });
                    document.getElementById('clip-bar').style.display = "none";
                    load(curPath);
                }

                async function rename(p, old) {
                    const n = prompt("Novo nome:", old);
                    if(n) {
                        const params = new URLSearchParams({ action: 'rename', path: p, dest: p.replace(old, n) });
                        await fetch('/api/action', { method: 'POST', body: params });
                        load(curPath);
                    }
                }

                async function del(p) { if(confirm('Apagar?')) { 
                    await fetch('/api/action', { method: 'POST', body: new URLSearchParams({action:'delete', path:p}) });
                    load(curPath); 
                } }

                async function mkdir() {
                    const n = prompt("Nome da pasta:");
                    if(n) {
                        await fetch('/api/action', { method: 'POST', body: new URLSearchParams({action:'mkdir', path:curPath, name:n}) });
                        load(curPath);
                    }
                }

                function goBack() { let p = curPath.split('/').filter(x=>x); p.pop(); load(p.join('/')); }
                async function upload() {
                    const fd = new FormData(); fd.append('file', document.getElementById('up').files[0]);
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