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
                body { font-family: -apple-system, system-ui, sans-serif; background: var(--bg); color: #fff; margin: 0; padding-bottom: 100px; }
                
                .header { background: rgba(0,0,0,0.8); backdrop-filter: blur(20px); sticky; top: 0; z-index: 100; padding: 20px 15px; border-bottom: 0.5px solid #333; }
                h1 { font-size: 32px; margin: 0; font-weight: 700; letter-spacing: -1px; }
                
                .search-bar { background: #1C1C1E; border-radius: 10px; padding: 10px 15px; margin: 15px; display: flex; align-items: center; color: #8E8E93; }
                input { background: transparent; border: none; color: #fff; width: 100%; font-size: 17px; outline: none; }

                .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; padding: 15px; }
                .file-card { text-align: center; }
                .folder-icon { width: 100%; aspect-ratio: 1/1; background: var(--card); border-radius: 15px; display: flex; align-items: center; justify-content: center; font-size: 45px; margin-bottom: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.3); position: relative; }
                .folder-icon img { width: 100%; height: 100%; object-fit: cover; border-radius: 15px; }
                .file-name { font-size: 13px; font-weight: 400; color: #FFF; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.2; }
                .file-size { font-size: 11px; color: #8E8E93; margin-top: 2px; }

                /* Menu de Contexto iOS */
                .context-menu { position: fixed; background: #2C2C2E; border-radius: 14px; width: 220px; display: none; flex-direction: column; z-index: 1000; overflow: hidden; box-shadow: 0 20px 50px rgba(0,0,0,0.5); }
                .menu-item { padding: 14px 20px; font-size: 17px; color: #fff; border-bottom: 0.5px solid #3A3A3C; }
                .menu-item:active { background: #3A3A3C; }

                .bottom-nav { position: fixed; bottom: 0; width: 100%; background: rgba(20,20,20,0.8); backdrop-filter: blur(20px); display: flex; justify-content: space-around; padding: 15px; border-top: 0.5px solid #333; }
            </style>
        </head>
        <body>
            <div class="header">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <h1>Explorar</h1>
                    <span onclick="goBack()" style="color:var(--blue); font-size:17px">Voltar</span>
                </div>
            </div>

            <div class="search-bar">
                <span style="margin-right:10px">🔍</span>
                <input type="text" id="search" placeholder="Buscar" oninput="filter()">
            </div>

            <div id="grid" class="grid"></div>

            <div id="context-menu" class="context-menu">
                <div class="menu-item" onclick="action('rename')">Renomear</div>
                <div class="menu-item" onclick="action('move')">Mover</div>
                <div class="menu-item" style="color:red" onclick="action('delete')">Apagar</div>
            </div>

            <div class="bottom-nav">
                <label>➕<input type="file" id="up" hidden onchange="upload()"></label>
                <span onclick="location.reload()">🔄</span>
                <span onclick="load('')">🏠</span>
            </div>

            <script>
                let curPath = "";
                let allFiles = [];
                let targetFile = "";
                let pressTimer;

                async function load(path = "") {
                    curPath = path;
                    const r = await fetch('/api/list?path=' + encodeURIComponent(path));
                    allFiles = await r.json();
                    render(allFiles);
                }

                function render(files) {
                    let h = '';
                    files.forEach(f => {
                        const isImg = ['jpg','jpeg','png','webp'].includes(f.name.split('.').pop().toLowerCase());
                        const iconHtml = isImg ? `<img src="/api/stream?path=${"$"}{encodeURIComponent(f.relPath)}">` : f.icon;
                        
                        h += `
                        <div class="file-card" 
                             ontouchstart="startPress(event, '${"$"}{f.relPath}')" 
                             ontouchend="endPress()" 
                             onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : "preview('"+f.relPath+"')" }">
                            <div class="folder-icon">${"$"}{iconHtml}</div>
                            <div class="file-name">${"$"}{f.name}</div>
                            <div class="file-size">${"$"}{f.size}</div>
                        </div>`;
                    });
                    document.getElementById('grid').innerHTML = h;
                }

                function startPress(e, path) {
                    targetFile = path;
                    pressTimer = setTimeout(() => {
                        const menu = document.getElementById('context-menu');
                        menu.style.display = 'flex';
                        menu.style.left = '50%';
                        menu.style.top = '50%';
                        menu.style.transform = 'translate(-50%, -50%)';
                        window.navigator.vibrate(50);
                    }, 500);
                }

                function endPress() { clearTimeout(pressTimer); }
                window.onclick = () => document.getElementById('context-menu').style.display = 'none';

                async function upload() {
                    const fd = new FormData(); fd.append('file', document.getElementById('up').files[0]);
                    await fetch('/upload?path='+encodeURIComponent(curPath), {method:'POST', body:fd});
                    load(curPath);
                }
                
                function goBack() { 
                    let p = curPath.split('/').filter(x=>x); p.pop(); load(p.join('/')); 
                }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}