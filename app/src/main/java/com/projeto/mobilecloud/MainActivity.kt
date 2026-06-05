// No setupUI(), adicione um painel de status abaixo do QR Code:
val storageInfo = FileUtils.getStorageInfo()
val statusPanel = TextView(this).apply {
    text = "SISTEMA NEXUS\nLivre: ${storageInfo.first}\nTotal: ${storageInfo.second}\nStatus: ONLINE"
    setTextColor(Color.GRAY); textSize = 12f; gravity = Gravity.CENTER; setPadding(0, 20, 0, 0)
}
sidebar.addView(statusPanel)

// No onCreate(), passe o contexto para o FileServer:
FileServer(this).start()