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
                :root { --ios-blue: #007AFF; --ios-bg: #000; --ios-card: #1C1C1E; }
                * { -webkit-tap-highlight-color: transparent; -webkit-touch-callout: none; user-select: none; }
                body { font-family: -apple-system, system-ui, sans-serif; background: var(--ios-bg); color: #fff; margin: 0; padding-bottom: 100px; }
                
                .header { background: rgba(0,0,0,0.8); backdrop-filter: blur(25px); position: sticky; top: 0; z-index: 1000; padding: 25px 20px; border-bottom: 0.5px solid #333; }
                h1 { font-size: 32px; margin: 0; font-weight: 800; letter-spacing: -0.5px; }

                .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; padding: 15px; }
                .folder-card { text-align: center; transition: transform 0.2s; }
                .folder-card:active { transform: scale(0.9); opacity: 0.7; }
                
                .icon-container { width: 100%; aspect-ratio: 1/1; background: var(--ios-card); border-radius: 20px; display: flex; align-items: center; justify-content: center; font-size: 42px; margin-bottom: 10px; box-shadow: 0 8px 25px rgba(0,0,0,0.4); overflow: hidden; }
                .icon-container img { width: 100%; height: 100%; object-fit: cover; }
                
                .name { font-size: 13px; font-weight: 500; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding: 0 5px; }
                
                .context-menu { position: fixed; background: rgba(44, 44, 46, 0.95); backdrop-filter: blur(20px); border-radius: 14px; width: 220px; display: none; flex-direction: column; z-index: 2000; box-shadow: 0 20px 60px rgba(0,0,0,0.6); border: 0.5px solid #444; }
                .menu-item { padding: 15px 20px; font-size: 17px; color: #fff; border-bottom: 0.5px solid #3A3A3C; }
                .menu-item:last-child { border: none; }
                
                .fab { position: fixed; bottom: 30px; right: 30px; width: 60px; height: 60px; background: var(--ios-blue); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 28px; box-shadow: 0 10px 30px rgba(0,122,255,0.4); z-index: 999; }
            </style>
        </head>
        <body>
            <div class="header">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <h1>Arquivos</h1>
                    <span onclick="goBack()" style="color:var(--ios-blue); font-size:17px">Voltar</span>
                </div>
            </div>

            <div id="grid" class="grid"></div>

            <div id="context-menu" class="context-menu">
                <div class="menu-item" onclick="action('rename')">Renomear</div>
                <div class="menu-item" onclick="action('move')">Mover</div>
                <div class="menu-item" style="color:#FF453A" onclick="action('delete')">Apagar</div>
            </div>

            <label class="fab">＋<input type="file" id="up" hidden onchange="upload()"></label>

            <script>
                let curPath = "";
                let targetFile = "";
                let pressTimer;

                async function load(path = "") {
                    curPath = path;
                    const r = await fetch('/api/list?path=' + encodeURIComponent(path));
                    const files = await r.json();
                    let h = '';
                    files.forEach(f => {
                        const isImg = ['jpg','jpeg','png','webp'].includes(f.name.split('.').pop().toLowerCase());
                        const thumb = isImg ? `<img src="/api/stream?path=${"$"}{encodeURIComponent(f.relPath)}">` : f.icon;
                        h += `<div class="folder-card" 
                                   ontouchstart="startPress('${"$"}{f.relPath}')" 
                                   ontouchend="endPress()" 
                                   onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : "preview('"+f.relPath+"')" }">
                                <div class="icon-container">${"$"}{thumb}</div>
                                <span class="name">${"$"}{f.name}</span>
                             </div>`;
                    });
                    document.getElementById('grid').innerHTML = h || "<p style='text-align:center; color:#444; grid-column: 1/4; margin-top:50px;'>Pasta Vazia ou Sem Permissão</p>";
                }

                function startPress(p) {
                    targetFile = p;
                    pressTimer = setTimeout(() => {
                        // GATILHO SENSORIAL: Vibração estilo iPhone
                        if (window.navigator.vibrate) window.navigator.vibrate(60);
                        
                        const m = document.getElementById('context-menu');
                        m.style.display = 'flex';
                        m.style.left = '50%';
                        m.style.top = '50%';
                        m.style.transform = 'translate(-50%, -50%)';
                    }, 600);
                }

                function endPress() { clearTimeout(pressTimer); }
                window.onclick = () => document.getElementById('context-menu').style.display = 'none';

                async function upload() {
                    const fd = new FormData(); fd.append('file', document.getElementById('up').files[0]);
                    await fetch('/upload?path=' + encodeURIComponent(curPath), { method: 'POST', body: fd });
                    load(curPath);
                }

                function goBack() { 
                    let p = curPath.split('/').filter(x => x); 
                    p.pop(); 
                    load(p.join('/')); 
                }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}