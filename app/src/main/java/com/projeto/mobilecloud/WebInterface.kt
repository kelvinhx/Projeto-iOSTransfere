package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html lang="pt-br">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>Arquivos</title>
            <style>
                :root { --blue: #007AFF; --bg: #000; --card: #1C1C1E; }
                * { -webkit-user-select: none; user-select: none; box-sizing: border-box; }
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: #fff; margin: 0; padding-bottom: 120px; }
                
                .header { background: rgba(0,0,0,0.8); backdrop-filter: blur(25px); position: sticky; top: 0; z-index: 100; padding: 25px 20px; border-bottom: 0.5px solid #333; }
                h1 { font-size: 32px; margin: 0; font-weight: 700; letter-spacing: -1px; }
                
                .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; padding: 15px; }
                .file-card { text-align: center; width: 100%; }
                
                /* Correção das Miniaturas */
                .folder-icon { width: 100%; aspect-ratio: 1/1; background: var(--card); border-radius: 18px; display: flex; align-items: center; justify-content: center; font-size: 40px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.4); }
                .folder-icon img { width: 100%; height: 100%; object-fit: cover; }
                
                .file-name { font-size: 12px; margin-top: 8px; color: #fff; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; height: 2.4em; }

                /* Menu Estilo iOS 18 */
                .context-menu { position: fixed; background: rgba(44,44,46,0.95); backdrop-filter: blur(20px); border-radius: 14px; width: 240px; display: none; flex-direction: column; z-index: 2000; box-shadow: 0 20px 60px rgba(0,0,0,0.8); }
                .menu-item { padding: 16px 20px; font-size: 17px; color: #fff; border-bottom: 0.5px solid rgba(255,255,255,0.1); display: flex; justify-content: space-between; }
                
                #clip-bar { position: fixed; bottom: 80px; left: 20px; right: 20px; background: var(--blue); padding: 15px; border-radius: 15px; display: none; justify-content: space-between; align-items: center; z-index: 500; }
                .btn-add { position: fixed; bottom: 20px; right: 20px; background: var(--blue); width: 60px; height: 60px; border-radius: 30px; display: flex; align-items: center; justify-content: center; font-size: 30px; box-shadow: 0 10px 30px rgba(0,122,255,0.4); }
            </style>
        </head>
        <body>
            <div class="header"><h1>Explorar</h1><small id="path-label" style="color:var(--blue)">/Início</small></div>
            
            <div id="grid" class="grid"></div>

            <div id="clip-bar">
                <span id="clip-txt" style="font-size:13px"></span>
                <button onclick="paste()" style="background:#fff; color:#000; border:none; padding:8px 15px; border-radius:10px; font-weight:bold">Colar Aqui</button>
            </div>

            <div id="context-menu" class="context-menu">
                <div class="menu-item" onclick="setClip('move')">Mover <span>✂️</span></div>
                <div class="menu-item" onclick="setClip('copy')">Copiar <span>📋</span></div>
                <div class="menu-item" onclick="rename()">Renomear <span>✏️</span></div>
                <div class="menu-item" style="color:var(--red)" onclick="del()">Apagar <span>🗑️</span></div>
            </div>

            <div class="btn-add"><label>+<input type="file" id="up" hidden onchange="upload()"></label></div>

            <script>
                let curPath = "", allFiles = [], target = {}, clip = null, pressTimer;
                let isScrolling = false;

                async fun load(p = "") {
                    curPath = p; document.getElementById('path-label').innerText = p || "/Raiz";
                    const r = await fetch('/api/list?path=' + encodeURIComponent(p));
                    allFiles = await r.json();
                    let h = '';
                    allFiles.forEach(f => {
                        const isImg = ['jpg','jpeg','png','webp'].includes(f.name.split('.').pop().toLowerCase());
                        const icon = isImg ? `<img src="/api/stream?path=${"$"}{encodeURIComponent(f.relPath)}">` : f.icon;
                        h += `<div class="file-card" 
                                   ontouchstart="startP(event, '${"$"}{f.relPath}', '${"$"}{f.name}')" 
                                   ontouchmove="isScrolling=true" 
                                   ontouchend="endP()" 
                                   onclick="if(!isScrolling)${"$"}{f.isDir ? "load('"+f.relPath+"')" : "openTV('"+f.relPath+"')" }">
                            <div class="folder-icon">${"$"}{icon}</div>
                            <div class="file-name">${"$"}{f.name}</div>
                        </div>`;
                    });
                    document.getElementById('grid').innerHTML = h;
                }

                function startP(e, p, n) { 
                    isScrolling = false; target = {p, n};
                    pressTimer = setTimeout(() => { 
                        if(isScrolling) return;
                        const m = document.getElementById('context-menu');
                        m.style.display = 'flex'; m.style.left = '50%'; m.style.top = '50%'; m.style.transform = 'translate(-50%, -50%)';
                        if(window.navigator.vibrate) window.navigator.vibrate(60);
                    }, 800); 
                }
                function endP() { clearTimeout(pressTimer); }
                window.onclick = () => document.getElementById('context-menu').style.display = 'none';

                function setClip(a) { clip = {a, p: target.p, n: target.n}; document.getElementById('clip-bar').style.display='flex'; document.getElementById('clip-txt').innerText = (a=='move'?'Mover ':'Copiar ') + target.n; }
                async function paste() {
                    const p = new URLSearchParams({ action: clip.a, path: clip.p, dest: curPath + '/' + clip.n });
                    await fetch('/api/action', { method: 'POST', body: p });
                    document.getElementById('clip-bar').style.display='none'; load(curPath);
                }
                async fun del() { if(confirm('Apagar?')) { await fetch('/api/action', { method:'POST', body:new URLSearchParams({action:'delete',path:target.p}) }); load(curPath); } }
                async fun upload() {
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