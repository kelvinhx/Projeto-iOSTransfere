// Dentro de routing { ... } post("/api/action") { ... }
val success = when(action) {
    "delete" -> source.deleteRecursively()
    "rename" -> source.renameTo(dest)
    "move" -> source.renameTo(dest) // No Java/Android move é um rename para outro path
    "mkdir" -> File(source, p["name"] ?: "Nova Pasta").mkdirs()
    else -> false
}