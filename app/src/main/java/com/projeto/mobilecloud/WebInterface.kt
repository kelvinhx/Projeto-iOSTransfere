package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html lang="pt-br">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>Nexus Transfer</title>
            <style>
                :root { --bg: #0a0a0c; --card: #1c1c1e; --accent: #0a84ff; --danger: #ff453a; }
                body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: var(--bg); color: white; margin: 0; padding: 20px; }
                .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
                .btn-upload { background: var(--accent); color: white; padding: 12px 20px; border-radius: 12px; font-weight: 600; cursor: pointer; display: inline-block; }
                .file-card { background: var(--card); padding: 15px; border-radius: 14px; margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center; animation: fadeIn 0.3s ease; }
                .file-info { display: flex; flex-direction: column; }
                .file-name { font-weight: 500; font-size: 16px; word-break: break-all; }
                .file-size { font-size: 12px; color: #8e8e93; }
                .btn-delete { color: var(--danger); font-size: 14px; font-weight: 600; background: none; border: none; padding: 10px; cursor: pointer; }
                #upload-status { font-size: 14px; color: var(--accent); margin-top: 10px; text-align: center; display: none; }
                @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
            </style>
        </head>
        <body>
            <div class="header">
                <h2 style="margin:0">Nexus Link</h2>
                <label for="fileInput" class="btn-upload">Enviar</label>
                <input type="file" id="fileInput" multiple style="display:none" onchange="uploadFiles()">
            </div>
            
            <div id="upload-status">Processando...</div>
            
            <div id="fileList">
                <!-- Arquivos aparecerão aqui -->
            </div>

            <script>
                async function loadFiles() {
                    const res = await fetch('/api/list');
                    const files = await res.json();
                    const container = document.getElementById('fileList');
                    container.innerHTML = files.length === 0 ? '<p style="color:#8e8e93;text-align:center">Nenhum arquivo na TV</p>' : '';
                    files.forEach(f => {
                        container.innerHTML += `
                            <div class="file-card">
                                <div class="file-info">
                                    <span class="file-name">${"$"}{f.name}</span>
                                    <span class="file-size">${"$"}{f.size}</span>
                                </div>
                                <button class="btn-delete" onclick="deleteFile('${"$"}{f.name}')">Apagar</button>
                            </div>
                        `;
                    });
                }

                async function uploadFiles() {
                    const input = document.getElementById('fileInput');
                    const status = document.getElementById('upload-status');
                    status.style.display = 'block';
                    const formData = new FormData();
                    for(let f of input.files) formData.append('file', f);
                    
                    await fetch('/upload', { method: 'POST', body: formData });
                    status.style.display = 'none';
                    input.value = '';
                    loadFiles();
                }

                async function deleteFile(name) {
                    if(!confirm('Deseja apagar ' + name + '?')) return;
                    const params = new URLSearchParams();
                    params.append('name', name);
                    await fetch('/api/delete', { method: 'POST', body: params });
                    loadFiles();
                }

                loadFiles(); // Carga inicial
                setInterval(loadFiles, 5000); // Atualiza a cada 5 segundos
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}