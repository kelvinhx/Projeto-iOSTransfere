package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nexus Web Explorer</title>
            <style>
                body { font-family: sans-serif; background: #000; color: #fff; margin: 0; padding: 15px; }
                .item { background: #1c1c1e; padding: 15px; border-radius: 10px; margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center; }
                .btn { background: #007aff; color: #fff; border: none; padding: 10px 20px; border-radius: 8px; font-weight: bold; }
                .toolbar { display: flex; gap: 10px; margin-bottom: 20px; position: sticky; top: 0; background: #000; padding: 10px 0; }
            </style>
        </head>
        <body>
            <div class="toolbar">
                <button class="btn" onclick="goBack()">⬅️ Voltar</button>
                <label class="btn" style="background:#32d74b">📤 Enviar <input type="file" id="u" hidden onchange="upload()"></label>
            </div>
            <div id="path" style="color:#007aff; margin-bottom:15px;">/Início</div>
            <div id="list">Carregando arquivos...</div>

            <script>
                let currentPath = "";

                async function load(path = "") {
                    currentPath = path;
                    document.getElementById('path').innerText = "📍 " + (path || "/Armazenamento Interno");
                    try {
                        const r = await fetch('/api/list?path=' + encodeURIComponent(path));
                        const files = await r.json();
                        let html = '';
                        files.forEach(f => {
                            html += `<div class="item" onclick="${"$"}{f.isDir ? "load('"+f.relPath+"')" : ""}">
                                <span>${"$"}{f.isDir ? '📂' : '📄'} ${"$"}{f.name}</span>
                                <button onclick="event.stopPropagation(); del('${"$"}{f.relPath}')" style="background:none;border:none;color:red;">🗑️</button>
                            </div>`;
                        });
                        document.getElementById('list').innerHTML = html || 'Pasta vazia ou sem permissão na TV.';
                    } catch (e) {
                        document.getElementById('list').innerHTML = 'Erro ao conectar. Verifique se o App está aberto na TV.';
                    }
                }

                function goBack() {
                    let p = currentPath.split('/').filter(x => x);
                    p.pop();
                    load(p.join('/'));
                }

                async function upload() {
                    const f = document.getElementById('u').files[0];
                    const fd = new FormData(); fd.append('file', f);
                    await fetch('/upload?path=' + encodeURIComponent(currentPath), { method: 'POST', body: fd });
                    load(currentPath);
                }

                async function del(p) {
                    if(confirm('Apagar?')) {
                        const params = new URLSearchParams(); params.append('action', 'delete'); params.append('path', p);
                        await fetch('/api/action', { method: 'POST', body: params });
                        load(currentPath);
                    }
                }

                load();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}