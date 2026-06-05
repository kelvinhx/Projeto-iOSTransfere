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
                :root { --ios-blue: #007AFF; --ios-bg: #F2F2F7; --ios-card: #FFFFFF; }
                @media (prefers-color-scheme: dark) { :root { --ios-bg: #000000; --ios-card: #1C1C1E; } }
                
                body { font-family: -apple-system, sans-serif; background: var(--ios-bg); color: currentColor; margin: 0; padding-bottom: 100px; }
                
                /* Header iOS 17 */
                .header { background: rgba(var(--ios-card), 0.7); backdrop-filter: blur(20px); position: sticky; top: 0; padding: 20px 15px; z-index: 100; display: flex; justify-content: space-between; align-items: center; }
                h1 { font-size: 34px; margin: 0; font-weight: 700; }
                
                .search-box { background: rgba(118, 118, 128, 0.12); border-radius: 10px; padding: 8px 12px; margin: 10px 15px; display: flex; align-items: center; }
                .search-input { border: none; background: transparent; color: inherit; width: 100%; font-size: 17px; outline: none; }

                /* Grid de Arquivos */
                .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; padding: 15px; }
                .file-card { text-align: center; position: relative; }
                .icon-box { background: var(--ios-card); aspect-ratio: 1/1; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 40px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); margin-bottom: 8px; overflow: hidden; }
                .icon-box img { width: 100%; height: 100%; object-fit: cover; }
                .name { font-size: 12px; font-weight: 400; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
                
                /* Menu de Contexto (Aperte e Segure) */
                .context-menu { position: fixed; background: var(--ios-card); border-radius: 12px; width: 200px; display: none; flex-direction: column; box-shadow: 0 10px 40px rgba(0,0,0,0.3); z-index: 1000; overflow: hidden; }
                .menu-item { padding: 12px 20px; border-bottom: 0.5px solid rgba(128,128,128,0.2); font-size: 17px; color: var(--ios-blue); }
                
                .bottom-nav { position: fixed; bottom: 0; width: 100%; background: rgba(var(--ios-card), 0.8); backdrop-filter: blur(20px); display: flex; justify-content: space-around; padding: 10px 0; border-top: 0.5px solid #333; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>Explorar</h1>
                <button onclick="goBack()" style="color:var(--ios-blue); background:none; border:none; font-size:17px;">⇠ Voltar</button>
            </div>

            <div class="search-box">
                <span style="margin-right:8px; opacity:0.5;">🔍</span>
                <input type="text" class="search-input" placeholder="Buscar">
            </div>

            <div id="grid" class="grid"></div>

            <div id="context-menu" class="context-menu">
                <div class="menu-item" onclick="action('move')">Mover</div>
                <div class="menu-item" onclick="action('rename')">Renomear</div>
                <div class="menu-item" onclick="action('delete')" style="color:var(--red)">Apagar</div>
            </div>

            <div class="bottom-nav">
                <label>➕<input type="file" id="up" hidden onchange="upload()"></label>
                <span onclick="location.reload()">🔄</span>
                <span onclick="alert('Espaço: ' + document.getElementById('storage').innerText)">📊</span>
            </div>

            <script>
                let currentPath = "";
                let longPressTimer;
                let targetPath = "";

                async function load(path = "") {
                    currentPath = path;
                    const r = await fetch('/api/list?path=' + encodeURIComponent(path));
                    const files = await r.json();
                    let html = '';
                    files.forEach(f => {
                        const isImg = ['jpg','jpeg','png'].includes(f.name.split('.').pop().toLowerCase());
                        const thumb = isImg ? `/api/stream?path=${"$"}{encodeURIComponent(f.relPath)}` : '';
                        
                        html += `
                        <div class="file-card" 
                             ontouchstart="startPress('${"$"}{f.relPath}')" 
                             ontouchend="endPress()" 
                             onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : "preview('"+f.relPath+"')" }">
                            <div class="icon-box">
                                ${"$"}{thumb ? `<img src="${"$"}{thumb}">` : f.icon}
                            </div>
                            <div class="name">${"$"}{f.name}</div>
                        </div>`;
                    });
                    document.getElementById('grid').innerHTML = html;
                }

                function startPress(path) {
                    targetPath = path;
                    longPressTimer = setTimeout(() => {
                        const menu = document.getElementById('context-menu');
                        menu.style.display = 'flex';
                        menu.style.left = '50%';
                        menu.style.top = '50%';
                        menu.style.transform = 'translate(-50%, -50%)';
                    }, 600);
                }

                function endPress() { clearTimeout(longPressTimer); }
                window.onclick = () => document.getElementById('context-menu').style.display = 'none';

                async function action(type) {
                    if(type === 'delete' && !confirm('Apagar?')) return;
                    await fetch('/api/action', { method: 'POST', body: new URLSearchParams({action: type, path: targetPath}) });
                    load(currentPath);
                }

                async function upload() {
                    const fd = new FormData(); fd.append('file', document.getElementById('up').files[0]);
                    await fetch('/upload?path='+encodeURIComponent(currentPath), {method:'POST', body:fd});
                    load(currentPath);
                }
                
                function goBack() { let p = currentPath.split('/').filter(x=>x); p.pop(); load(p.join('/')); }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}