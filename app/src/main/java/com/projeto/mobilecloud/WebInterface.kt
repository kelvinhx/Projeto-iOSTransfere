package com.projeto.mobilecloud

object WebInterface {
    fun getHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nexus Transfer</title>
            <style>
                body { font-family: -apple-system, sans-serif; background: #0F111A; color: white; margin: 0; padding: 20px; text-align: center; }
                .container { max-width: 500px; margin: auto; background: #1A1D2E; padding: 30px; border-radius: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                h1 { color: #00E5FF; }
                .drop-zone { border: 2px dashed #00E5FF; padding: 40px; border-radius: 15px; margin: 20px 0; cursor: pointer; }
                input[type="file"] { display: none; }
                .btn { background: #00E5FF; color: #0F111A; padding: 15px 30px; border-radius: 50px; font-weight: bold; text-decoration: none; display: inline-block; transition: 0.3s; }
                #status { margin-top: 20px; font-weight: bold; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>Nexus Link</h1>
                <p>Transfira arquivos para sua TV instantaneamente.</p>
                <div class="drop-zone" onclick="document.getElementById('f').click()">
                    Clique aqui para selecionar arquivos
                    <input type="file" id="f" multiple onchange="u()">
                </div>
                <div id="status">Aguardando arquivo...</div>
            </div>
            <script>
                function u() {
                    const input = document.getElementById('f');
                    const status = document.getElementById('status');
                    const fd = new FormData();
                    for(let i=0; i<input.files.length; i++) {
                        fd.append('f', input.files[i]);
                    }
                    status.innerText = 'Enviando...';
                    status.style.color = '#FFD600';
                    fetch('/upload', {method:'POST', body:fd})
                    .then(r => r.text())
                    .then(d => {
                        status.innerText = d;
                        status.style.color = '#00FF00';
                        input.value = '';
                    })
                    .catch(e => {
                        status.innerText = 'Erro na conexão';
                        status.style.color = '#FF5252';
                    });
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}