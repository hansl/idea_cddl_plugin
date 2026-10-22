package online.larsen.cddl.psi

import com.intellij.openapi.util.TextRange

/** Helpers for CDDL text (`"..."`) and byte (`'...'`, `h'...'`, `b64'...'`) string literals. */
object CddlStringUtil {
    class Decoded(
        val value: String,
        /** For each char of [value] (plus one for the end), its offset in the encoded text. */
        val offsets: IntArray,
        /** Ranges, in the encoded text, of valid escape sequences. */
        val validEscapes: List<TextRange>,
        /** Ranges, in the encoded text, of escape sequences that are not valid. */
        val invalidEscapes: List<TextRange>,
    )

    /** The range of the literal's content: the text between the quotes, after any application prefix. */
    fun contentRange(literal: String): TextRange {
        val open = literal.indexOfFirst { it == '"' || it == '\'' }
        if (open < 0) return TextRange.EMPTY_RANGE
        val quote = literal[open]
        val terminated = literal.length > open + 1 && literal.last() == quote && !isEscaped(literal, literal.length - 1)
        return TextRange(open + 1, if (terminated) literal.length - 1 else literal.length)
    }

    fun isTerminated(literal: String): Boolean {
        val range = contentRange(literal)
        return range.endOffset < literal.length
    }

    /** The application prefix of a byte string (`h`, `b64`, ...), or an empty string. */
    fun prefix(literal: String): String = literal.takeWhile { it != '"' && it != '\'' }

    private fun isEscaped(text: String, index: Int): Boolean {
        var backslashes = 0
        var i = index - 1
        while (i >= 0 && text[i] == '\\') {
            backslashes++
            i--
        }
        return backslashes % 2 == 1
    }

    /**
     * Decodes the escape sequences of a text string's content (RFC 9682 section 2.1): the JSON escapes `\" \/ \\ \b
     * \f \n \r \t \uXXXX` and `\u{X...}`. Other escaped characters are kept as is, as RFC 8610 allowed.
     */
    fun decode(content: String): Decoded {
        val out = StringBuilder(content.length)
        val offsets = ArrayList<Int>(content.length + 1)
        val valid = mutableListOf<TextRange>()
        val invalid = mutableListOf<TextRange>()
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (c != '\\') {
                out.append(c)
                offsets.add(i)
                i++
                continue
            }
            if (i + 1 >= content.length) {
                invalid.add(TextRange(i, i + 1))
                break
            }
            val start = i
            val decoded: String
            when (val e = content[i + 1]) {
                '"', '/', '\\', '\'' -> { decoded = e.toString(); i += 2 }
                'b' -> { decoded = "\b"; i += 2 }
                'f' -> { decoded = "\u000c"; i += 2 }
                'n' -> { decoded = "\n"; i += 2 }
                'r' -> { decoded = "\r"; i += 2 }
                't' -> { decoded = "\t"; i += 2 }
                'u' -> {
                    val (codePoint, end) = parseUnicodeEscape(content, i + 2)
                    if (codePoint == null) {
                        invalid.add(TextRange(start, end))
                        decoded = content.substring(start, end)
                    } else {
                        decoded = String(Character.toChars(codePoint))
                    }
                    i = end
                }
                else -> {
                    invalid.add(TextRange(start, start + 2))
                    decoded = e.toString()
                    i += 2
                }
            }
            if (invalid.lastOrNull()?.startOffset != start) valid.add(TextRange(start, i))
            out.append(decoded)
            repeat(decoded.length) { offsets.add(start) }
        }
        offsets.add(content.length)
        return Decoded(out.toString(), offsets.toIntArray(), valid, invalid)
    }

    /** Parses the part after `\u`; returns the code point (or null when invalid) and the end offset. */
    private fun parseUnicodeEscape(text: String, from: Int): Pair<Int?, Int> {
        if (from < text.length && text[from] == '{') {
            val close = text.indexOf('}', from)
            if (close < 0) return null to text.length
            val hex = text.substring(from + 1, close)
            val value = hex.takeIf { it.isNotEmpty() && it.all(::isHexDigit) }?.toBigInteger(16)
            val codePoint = value?.takeIf { it <= 0x10FFFF.toBigInteger() }?.toInt()
            val valid = codePoint != null && codePoint !in 0xD800..0xDFFF
            return (if (valid) codePoint else null) to close + 1
        }
        val end = from + 4
        if (end > text.length || !text.substring(from, end).all(::isHexDigit)) {
            return null to minOf(end, text.length)
        }
        return text.substring(from, end).toInt(16) to end
    }

    private fun isHexDigit(c: Char): Boolean = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
}
