package dev.typetype.android.core.url

private const val HEX = "0123456789ABCDEF"

internal fun percentEncode(value: String): String {
    val bytes = value.toByteArray(Charsets.UTF_8)
    val encoded = StringBuilder(bytes.size)
    for (byte in bytes) {
        val code = byte.toInt() and 0xff
        val character = code.toChar()
        if (character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' ||
            character == '-' || character == '_' || character == '.' || character == '~'
        ) {
            encoded.append(character)
        } else {
            encoded.append('%')
                .append(HEX[(code shr 4) and 0x0f])
                .append(HEX[code and 0x0f])
        }
    }
    return encoded.toString()
}
