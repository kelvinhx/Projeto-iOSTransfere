package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nexus Pro Explorer</title>
            <style>
                :root { --blue: #007AFF; --green: #34C759; --bg: #000; }
                body { font-family: sans-serif; background: var(--bg); color: #fff; margin: 0; padding: 15px; }
                .item { background: #1c1c1e; padding: 15px; border-radius: 12px; margin-bottom: 10px; display: flex; justify-content: space-between; }
                .btn { background: var(--blue); color: #fff; border: none; padding: 12px 20px; border-radius: 10px; font-weight: bold; }
                #progress-container { display: none; background: #1c1c1e; padding: 20px; border-radius: 15px; position: fixed; bottom: 20px; left: 15px; right: 15px; box-shadow: 0 -5px 20px rgba(0,0,0,0.5); border: 1px solid var(--blue); }
                .bar-bg { background: #333; height: 10px; border-radius: 5px; margin-top: 10px; overflow: hidden; }
                .bar-fill { background: var(--blue); height: 100%; width: 0%; transition: 0.2s; }
                .toast { position: fixed; top: 20px; left: 50%; transform: translateX(-50%); background: var(--green); padding: 10px 20px; border-radius: 20px; display: none; animation: slideIn 0.3s; }
                @keyframes slideIn { from { top: -50px; } to { top: 20px; } }
            </style>
        </head>
        <body>
            <div id="toast" class="toast">✅ Transferência Concluída!</div>
            <div style="display:flex; gap:10px; margin-bottom:20px;">
                <button class="btn" onclick="goBack()">⬅️</button>
                <label class="btn" style="background:var(--green)">📤 Enviar <input type="file" id="up" hidden onchange="upload()"></label>
            </div>
            
            <div id="list">Carregando...</div>

            <div id="progress-container">
                <div style="display:flex; justify-content:space-between">
                    <span id="file-name" style="font-size:14px">Enviando...</span>
                    <span id="percent">0%</span>
                </div>
                <div class="bar-bg"><div id="bar-fill" class="bar-fill"></div></div>
            </div>

            <script>
                let currentPath = "";

                async function load(path = "") {
                    currentPath = path;
                    const r = await fetch('/api/list?path=' + encodeURIComponent(path));
                    const files = await r.json();
                    let html = '';
                    files.forEach(f => {
                        html += `<div class="item" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                            <span>${"$"}{f.isDir ? '📂' : '📄'} ${"$"}{f.name}</span>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = html;
                }

                function upload() {
                    const file = document.getElementById('up').files[0];
                    const fd = new FormData(); fd.append('file', file);
                    const container = document.getElementById('progress-container');
                    const fill = document.getElementById('bar-fill');
                    const percent = document.getElementById('percent');
                    const toast = document.getElementById('toast');

                    container.style.display = 'block';
                    document.getElementById('file-name').innerText = file.name;

                    const xhr = new XMLHttpRequest();
                    xhr.open('POST', '/upload?path=' + encodeURIComponent(currentPath));
                    
                    xhr.upload.onprogress = e => {
                        const p = Math.round((e.loaded / e.total) * 100);
                        fill.style.width = p + '%';
                        percent.innerText = p + '%';
                    };

                    xhr.onload = () => {
                        container.style.display = 'none';
                        toast.style.display = 'block';
                        setTimeout(() => toast.style.display = 'none', 3000);
                        load(currentPath);
                    };
                    xhr.send(fd);
                }

                function goBack() {
                    let p = currentPath.split('/').filter(x => x); p.pop();
                    load(p.join('/'));
                }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}