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
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: #fff; margin: 0; padding-bottom: 100px; }
                .header { background: rgba(0,0,0,0.8); backdrop-filter: blur(20px); position: sticky; top: 0; padding: 20px 15px; border-bottom: 0.5px solid #333; z-index: 100; }
                .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; padding: 15px; }
                .file-card { text-align: center; }
                .folder-icon { width: 100%; aspect-ratio: 1/1; background: var(--card); border-radius: 15px; display: flex; align-items: center; justify-content: center; font-size: 40px; margin-bottom: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.3); overflow: hidden; }
                .folder-icon img { width: 100%; height: 100%; object-fit: cover; }
                .file-name { font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; }
                .file-size { font-size: 11px; color: #8E8E93; }
                .context-menu { position: fixed; background: #2C2C2E; border-radius: 14px; width: 200px; display: none; flex-direction: column; z-index: 1000; box-shadow: 0 20px 50px rgba(0,0,0,0.5); }
                .menu-item { padding: 15px; border-bottom: 0.5px solid #3A3A3C; color: #fff; text-align: center; }
            </style>
        </head>
        <body>
            <div class="header"><div style="display:flex; justify-content:space-between; align-items:center;"><h1>Explorar</h1><span onclick="goBack()" style="color:var(--blue)">Voltar</span></div></div>
            <div id="grid" class="grid"></div>
            <div id="context-menu" class="context-menu">
                <div class="menu-item" onclick="action('rename')">Renomear</div>
                <div class="menu-item" style="color:red" onclick="action('delete')">Apagar</div>
            </div>
            <div style="position:fixed; bottom:20px; right:20px;"><label style="background:var(--blue); padding:15px; border-radius:50%; font-size:24px;">➕<input type="file" id="up" hidden onchange="upload()"></label></div>

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
                        const iconHtml = isImg ? `<img src="/api/stream?path=${"$"}{encodeURIComponent(f.relPath)}">` : f.icon;
                        h += `<div class="file-card" ontouchstart="startPress('${"$"}{f.relPath}')" ontouchend="endPress()" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                            <div class="folder-icon">${"$"}{iconHtml}</div>
                            <div class="file-name">${"$"}{f.name}</div>
                            <div class="file-size">${"$"}{f.size}</div>
                        </div>`;
                    });
                    document.getElementById('grid').innerHTML = h;
                }
                function startPress(p) { targetFile = p; pressTimer = setTimeout(() => { 
                    const m = document.getElementById('context-menu');
                    m.style.display = 'flex'; m.style.left = '50%'; m.style.top = '50%'; m.style.transform = 'translate(-50%, -50%)';
                }, 600); }
                function endPress() { clearTimeout(pressTimer); }
                window.onclick = () => document.getElementById('context-menu').style.display = 'none';
                async function upload() {
                    const fd = new FormData(); fd.append('file', document.getElementById('up').files[0]);
                    await fetch('/upload?path='+encodeURIComponent(curPath), {method:'POST', body:fd}); load(curPath);
                }
                function goBack() { let p = curPath.split('/').filter(x=>x); p.pop(); load(p.join('/')); }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}