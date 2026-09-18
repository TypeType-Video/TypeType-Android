package video.typetype.tv.data

internal fun reorderedVideoIds(ids: List<String>, selectedId: String, offset: Int): List<String>? {
    val source = ids.indexOf(selectedId)
    if (source < 0 || ids.isEmpty()) return null
    val destination = (source + offset).coerceIn(0, ids.lastIndex)
    if (source == destination) return null
    return ids.toMutableList().apply {
        add(destination, removeAt(source))
    }
}
