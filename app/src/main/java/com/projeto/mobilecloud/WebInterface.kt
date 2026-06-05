// Dentro da função render(data), adicione o botão de abrir:
h += `<div class="item">
    <div style="flex:1" onclick="${f.isDir ? "load('"+f.relPath+"')" : ""}">
        ${f.icon} ${f.name} <br><small style="color:#666">${f.size}</small>
    </div>
    <div style="display:flex; gap:10px">
        ${!f.isDir ? `<button class="btn" style="background:#5856D6; padding:5px 10px" onclick="openOnTV('${f.relPath}')">▶️</button>` : ''}
        <button class="btn" style="background:red; padding:5px 10px" onclick="del('${f.relPath}')">🗑️</button>
    </div>
</div>`;

// Adicione a função de chamada à API:
async function openOnTV(path) {
    const r = await fetch('/api/open?path=' + encodeURIComponent(path));
    if(r.ok) alert("Abrindo na TV...");
}