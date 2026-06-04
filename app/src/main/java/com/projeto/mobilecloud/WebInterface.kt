package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <title>Nexus Pro</title>
            <style>
                :root { --bg: #000; --card: #151515; --blue: #007AFF; }
                body { font-family: sans-serif; background: var(--bg); color: #fff; margin: 0; padding: 15px; }
                .item { background: var(--card); padding: 15px; border-radius: 12px; margin-bottom: 8px; display: flex; align-items: center; justify-content: space-between; }
                .btn { background: var(--blue); border: none; color: #fff; padding: 10px 20px; border-radius: 8px; font-weight: bold; }
                .nav { margin-bottom: 20px; font-size: 14px; color: var(--blue); }
                #progress { width: 100%; height: 4px; background: #333; display: none; margin: 10px 0; }
                #bar { width: 0%; height: 100%; background: var(--blue); transition: 0.3s; }
            </style>
        </head>
        <body>
            <h3>📁 Gerenciador Nexus</h3>
            <div id="path" class="nav">/Downloads</div>
            
            <div style="display:flex; gap:10px; margin-bottom:20px;">
                <label class="btn">📤 Enviar <input type="file" id="f" hidden onchange="u()"></label>
                <button class="btn" style="background:#333" onclick="mkdir()">📁 +Pasta</button>
            </div>

            <div id="progress"><div id="bar"></div></div>
            <div id="list"></div>

            <script>
                let curPath = "";
                async function load() {
                    const r = await fetch('/api/list?path=' + curPath);
                    const files = await r.json();
                    let html = '';
                    files.forEach(f => {
                        html += `<div class="item">
                            <div onclick="${"$"}{f.isDir ? "enter('"+f.name+"')" : ""}">
                                ${"$"}{f.isDir ? '📂' : '📄'} ${"$"}{f.name} <br>
                                <small style="color:#666">${"$"}{f.size}</small>
                            </div>
                            <button style="background:none;border:none;color:red;" onclick="op('delete', '${"$"}{f.name}')">🗑️</button>
                        </div>`;
                    });
                    document.getElementById('list').innerHTML = html;
                    document.getElementById('path').innerText = "Pasta: " + (curPath || "/Downloads");
                }

                function enter(name) { curPath += (curPath ? "/" : "") + name; load(); }
                
                async function op(action, name, newName="") {
                    const p = new URLSearchParams();
                    p.append('action', action); p.append('name', curPath + (curPath?"/":"") + name); p.append('newName', newName);
                    await fetch('/api/op', {method:'POST', body:p});
                    load();
                }

                function mkdir() { let n = prompt("Nome da pasta:"); if(n) op('mkdir', n); }

                async function u() {
                    const f = document.getElementById('f').files[0];
                    const fd = new FormData();
                    fd.append('file', f);
                    document.getElementById('progress').style.display = 'block';
                    
                    const xhr = new XMLHttpRequest();
                    xhr.open('POST', '/upload?path=' + curPath);
                    xhr.upload.onprogress = (e) => {
                        document.getElementById('bar').style.width = (e.loaded/e.total*100) + '%';
                    };
                    xhr.onload = () => { 
                        alert('✅ Sucesso!');
                        document.getElementById('progress').style.display = 'none';
                        load(); 
                    };
                    xhr.send(fd);
                }
                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}