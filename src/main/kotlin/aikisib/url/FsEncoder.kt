package aikisib.url

import java.nio.ByteBuffer
import java.nio.CharBuffer

/**
 * Кодирует части пути URI (то, что между "/") в виде, пригодном для сохранения
 * в файловой системе (NTFS, extX). То, есть, юникодные символы _не_ кодируются.
 * Знаки минус ("-"), подчерка ("_") и точки (".") тоже _не_ кодируются.
 */
object FsEncoder {

    fun encode(input: String): String {
        val encoder = Charsets.UTF_8.newEncoder()
        val charBuffer = CharBuffer.allocate(SINGLE_UNICODE_CHAR)
        val byteBuffer = ByteBuffer.allocate(FOUR_BYTES_IN_SINGLE_UNICODE_CHAR_MAX)
        return buildString {
            // только двухбайтные символы
            input.forEach { char ->
                charBuffer.put(char).flip()
                val result = encoder.encode(charBuffer, byteBuffer, true)
                require(!result.isError) { "не получилось закодировать строку $input в байты" }
                when {
                    byteBuffer.position() != 1 && char.isLetter() -> append(char)
                    else -> maybeEncodeAppending(byteBuffer)
                }
                charBuffer.clear()
                byteBuffer.clear()
            }
        }
    }

    private fun StringBuilder.maybeEncodeAppending(byteBuffer: ByteBuffer) =
        byteBuffer.forEach { maybeEncodeAppending(it) }

    private fun StringBuilder.maybeEncodeAppending(it: Byte) {
        when (it) {
            SPACE -> append("%20")
            in URL_ALPHABET -> append(it.toInt().toChar())
            else -> append(it.percentEncode())
        }
    }

    @Suppress("MagicNumber")
    private fun Byte.percentEncode(): String = buildString(3) {
        val code = this@percentEncode.toInt() and 0xff
        append('%')
        append(hexDigitToChar(code shr 4))
        append(hexDigitToChar(code and 0x0f))
    }

    @Suppress("MagicNumber")
    private fun hexDigitToChar(digit: Int): Char = when (digit) {
        in 0..9 -> '0' + digit
        else -> 'A' + digit - 10
    }

    private fun ByteBuffer.forEach(block: (Byte) -> Unit) {
        for (counter in 0 until position()) {
            block(this[counter])
        }
    }

    private val URL_ALPHABET: List<Byte> = buildList {
        addAll(('a'..'z'))
        addAll(('A'..'Z'))
        addAll(('0'..'9'))
        add('-')
        add('_')
        add('.')
    }.map { it.code.toByte() }

    private const val SPACE: Byte = ' '.code.toByte()
    private const val SINGLE_UNICODE_CHAR = 1
    private const val FOUR_BYTES_IN_SINGLE_UNICODE_CHAR_MAX = 4
}
