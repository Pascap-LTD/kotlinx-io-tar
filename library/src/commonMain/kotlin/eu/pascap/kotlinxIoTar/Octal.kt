/*
 * Copyright (c) 2026 KotlinxIoTar Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.pascap.kotlinxIoTar

object Octal {

    const val ZERO = '0'.code.toByte()
    const val SPACE = ' '.code.toByte()

    /**
     * Parse an octal string from a header buffer. This is used for the file
     * permission mode value.
     *
     * @param header
     * The header buffer from which to parse.
     * @param offset
     * The offset into the buffer from which to parse.
     * @param length
     * The number of header bytes to parse.
     *
     * @return The long value of the octal string.
     */
    fun parseOctal(header: ByteArray, offset: Int, length: Int): Long {
        var result: Long = 0
        var stillPadding = true

        val end = offset + length
        for (i in offset..<end) {
            if (header[i].toInt() == 0) break

            if (header[i] == SPACE || header[i] == ZERO) {
                if (stillPadding) continue

                if (header[i] == SPACE) break
            }

            stillPadding = false

            result = (result shl 3) + (header[i] - ZERO)
        }

        return result
    }

    /**
     * Write an octal integer to a header buffer.
     *
     * @param value
     * The value to write.
     * @param buf
     * The header buffer from which to parse.
     * @param offset
     * The offset into the buffer from which to parse.
     * @param length
     * The number of header bytes to parse.
     *
     * @return The integer value of the octal bytes.
     */
    fun getOctalBytes(value: Long, buf: ByteArray, offset: Int, length: Int): Int {
        var idx = length - 1

        buf[offset + idx] = 0
        --idx
        buf[offset + idx] = SPACE
        --idx

        if (value == 0L) {
            buf[offset + idx] = ZERO
            --idx
        } else {
            var currentVal = value
            while (idx >= 0 && currentVal > 0) {
                buf[offset + idx] = (ZERO + (currentVal and 7L).toByte()).toByte()
                currentVal = currentVal shr 3
                --idx
            }
        }

        while (idx >= 0) {
            buf[offset + idx] = ZERO
            --idx
        }

        return offset + length
    }

    /**
     * Write the checksum octal integer to a header buffer.
     *
     * @param value
     * The value to write.
     * @param buf
     * The header buffer from which to parse.
     * @param offset
     * The offset into the buffer from which to parse.
     * @param length
     * The number of header bytes to parse.
     * @return The integer value of the entry's checksum.
     */
    fun getCheckSumOctalBytes(value: Long, buf: ByteArray, offset: Int, length: Int): Int {
        getOctalBytes(value, buf, offset, length)
        buf[offset + length - 1] = SPACE
        buf[offset + length - 2] = 0
        return offset + length
    }

    /**
     * Write an octal long integer to a header buffer.
     *
     * @param value
     * The value to write.
     * @param buf
     * The header buffer from which to parse.
     * @param offset
     * The offset into the buffer from which to parse.
     * @param length
     * The number of header bytes to parse.
     *
     * @return The long value of the octal bytes.
     */
    fun getLongOctalBytes(value: Long, buf: ByteArray, offset: Int, length: Int): Int {
        val temp = ByteArray(length + 1)
        getOctalBytes(value, temp, 0, length + 1)
        temp.copyInto(buf, offset, 0, length)

        return offset + length
    }
}
