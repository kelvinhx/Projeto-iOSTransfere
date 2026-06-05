package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                :root { --blue: #007AFF; --bg: #000; --card: #1C1C1E; }
                * { -webkit-tap-highlight-color: transparent; outline: none; }
                body { font-family: -apple-system, sans-serif; background: var(--bg); color: #fff; margin: 0; padding-bottom: 100px; }
                .header { background: rgba(0,0,0,0.8); backdrop-filter: blur(30px); position: sticky; top: 0; padding: 30px 20px; z-index: 1000; border-bottom: 0.5px solid #333; }
                h1 { font-size: 34px; margin: 0; font-weight: 800; letter-spacing: -1px; }
                .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; padding: 20px; }
                .card { text-align: center; animation: slideIn 0.4s ease; }
                .icon { width: 100%; aspect-ratio: 1/1; background: var(--card); border-radius: 22px; display: flex; align-items: center; justify-content: center; font-size: 45px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); margin-bottom: 10px; overflow: hidden; }
                .icon img { width: 100%; height: 100%; object-fit: cover; }
                .name { font-size: 13px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; width: 100%; display: block; }
                .context-menu { position: fixed; background: #2C2C2E; border-radius: 18px; width: 250px; display: none; flex-direction: column; z-index: 2000; box-shadow: 0 30px 60px rgba(0,0,0,0.8); }
                .menu-item { padding: 18px; border-bottom: 0.5px solid #3A3A3C; font-size: 18px; }
                @keyframes slideIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
            </style>
        </head>
        <body>
            <div class="header">
                <div style="display:flex; justify-content:space-between; align-items:center">
                    <h1>Arquivos</h1>
                    <span onclick="goBack()" style="color:var(--blue); font-size:18px">Voltar</span>
                </div>
            </div>

            <div id="grid" class="grid"></div>

            <div id="context-menu" class="context-menu">
                <div class="menu-item" onclick="action('rename')">Renomear</div>
                <div class="menu-item" style="color:#FF453A" onclick="action('delete')">Apagar</div>
            </div>

            <div style="position:fixed; bottom:30px; right:30px; z-index:1000">
                <label style="background:var(--blue); width:65px; height:65px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:30px; box-shadow: 0 10px 30px rgba(0,122,255,0.4)">
                    ＋<input type="file" id="up" hidden onchange="upload()">
                </label>
            </div>

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
                        h += `<div class="card" ontouchstart="startPress('${"$"}{f.relPath}')" ontouchend="endPress()" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                                <div class="icon">${"$"}{thumb}</div>
                                <div class="name">${"$"}{f.name}</div>
                             </div>`;
                    });
                    document.getElementById('grid').innerHTML = h;
                }
                function startPress(p) { targetFile = p; pressTimer = setTimeout(() => {
                    const m = document.getElementById('context-menu');
                    m.style.display='flex'; m.style.left='50%'; m.style.top='50%'; m.style.transform='translate(-50%, -50%)';
                }, 600); }
                function endPress() { clearTimeout(pressTimer); }
                window.onclick = () => document.getElementById('context-menu').style.display='none';
                async function upload() {
                    const fd = new FormData(); fd.append('file', document.getElementById('up').files[0]);
                    await fetch('/upload?path='+encodeURIComponent(curPath), {method:'POST', body:fd});
                    load(curPath);
                }
                function goBack() { let p = curPath.split('/').filter(x=>x); p.pop(); load(p.join('/')); }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}