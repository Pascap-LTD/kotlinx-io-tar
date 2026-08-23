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

import kotlinx.io.bytestring.buildByteString
import kotlinx.io.bytestring.decodeToString

/**
 * Header
 *
 * <pre>
 * Offset  Size     Field
 * 0       100      File name
 * 100     8        File mode
 * 108     8        Owner's numeric user ID
 * 116     8        Group's numeric user ID
 * 124     12       File size in bytes
 * 136     12       Last modification time in numeric Unix time format
 * 148     8        Checksum for header block
 * 156     1        Link indicator (file type)
 * 157     100      Name of linked file
</pre> *
 *
 *
 * File Types
 *
 * <pre>
 * Value        Meaning
 * '0'          Normal file
 * (ASCII NUL)  Normal file (now obsolete)
 * '1'          Hard link
 * '2'          Symbolic link
 * '3'          Character special
 * '4'          Block special
 * '5'          Directory
 * '6'          FIFO
 * '7'          Contigous
</pre> *
 *
 *
 *
 * Ustar header
 *
 * <pre>
 * Offset  Size    Field
 * 257     6       UStar indicator "ustar"
 * 263     2       UStar version "00"
 * 265     32      Owner user name
 * 297     32      Owner group name
 * 329     8       Device major number
 * 337     8       Device minor number
 * 345     155     Filename prefix
</pre> *
 */
class TarHeader {
    // Header values
    var name: String
    var mode: Int = 0
    var userId: Int
    var groupId: Int
    var size: Long = 0
    var modTime: Long = 0
    var checkSum: Int = 0
    var linkFlag: Byte = 0
    var linkName: String
    var magic: String // ustar indicator and version
    var userName: String
    var groupName: String
    var devMajor: Int = 0
    var devMinor: Int = 0
    var namePrefix: String

    init {
        this.magic = USTAR_MAGIC

        this.name = ""
        this.linkName = ""

        var user = ""

        this.userId = 0
        this.groupId = 0
        this.userName = user
        this.groupName = ""
        this.namePrefix = ""
    }

    companion object {
        /*
         * Header
         */
        const val NAME_LEN = 100
        const val MODE_LEN = 8
        const val UID_LEN = 8
        const val GID_LEN = 8
        const val SIZE_LEN = 12
        const val MOD_TIME_LEN = 12
        const val CHK_SUM_LEN = 8
        const val LF_OLD_NORM: Byte = 0

        /*
         * File Types
         */
        const val LF_NORMAL: Byte = '0'.code.toByte()
        const val LF_LINK: Byte = '1'.code.toByte()
        const val LF_SYMLINK: Byte = '2'.code.toByte()
        const val LF_CHR: Byte = '3'.code.toByte()
        const val LF_BLK: Byte = '4'.code.toByte()
        const val LF_DIR: Byte = '5'.code.toByte()
        const val LF_FIFO: Byte = '6'.code.toByte()
        const val LF_CONTIG: Byte = '7'.code.toByte()

        /*
         * Ustar header
         */
        const val USTAR_MAGIC: String = "ustar" // POSIX

        const val USTAR_MAGIC_LEN: Int = 8
        const val USTAR_USER_NAME_LEN: Int = 32
        const val USTAR_GROUP_NAME_LEN: Int = 32
        const val USTAR_DEV_LEN: Int = 8
        const val USTAR_FILENAME_PREFIX: Int = 155

        /**
         * Parse an entry name from a header buffer.
         *
         * @param header
         * The header buffer from which to parse.
         * @param offset
         * The offset into the buffer from which to parse.
         * @param length
         * The number of header bytes to parse.
         * @return The header's entry name.
         */
        fun parseName(header: ByteArray, offset: Int, length: Int): String {
            val end = offset + length
            return buildByteString {
                for (i in offset..<end) {
                    if (header[i].toInt() == 0) break
                    append(header[i])
                }
            }.decodeToString()
        }

        /**
         * Determine the number of bytes in an entry name.
         *
         * @param name
         * The header buffer from which to parse.
         * @param offset
         * The offset into the buffer from which to parse.
         * @param length
         * The number of header bytes to parse.
         * @return The number of bytes in a header's entry name.
         */
        fun getNameBytes(name: String, buf: ByteArray, offset: Int, length: Int): Int {
            val nameBytes = mutableListOf<Byte>()

            for (i in name.indices) {
                // convert UTF16 chars to UTF8
                val utf8Bytes = name[i].toString().encodeToByteArray().toList()

                if (nameBytes.size + utf8Bytes.size > length) {
                    // break if impossible to add whole multibyte character
                    // to avoid code points splitting
                    break
                }

                nameBytes.addAll(utf8Bytes)
            }

            var i = 0
            while (i < length && i < nameBytes.size) {
                buf[offset + i] = nameBytes[i]
                ++i
            }

            while (i < length) {
                buf[offset + i] = 0
                ++i
            }

            return offset + length
        }

        /**
         * Creates a new header for a file/directory entry.
         *
         *
         * @param entryName
         * File name
         * @param size
         * File size in bytes
         * @param modTime
         * Last modification time in numeric Unix time format
         * @param dir
         * Is directory
         */
        fun createHeader(entryName: String, size: Long, modTime: Long, dir: Boolean, permissions: Int): TarHeader {
            var name = entryName
            // replace any non-standard file separators with forward slashes
            name = name.replace('\\', '/').trim('/')

            val header = TarHeader()
            header.linkName = ""
            header.mode = permissions

            if (name.length > 100) {
                header.namePrefix = name.substring(0, name.lastIndexOf('/'))
                header.name = name.substring(name.lastIndexOf('/') + 1)
            } else {
                header.name = name
            }
            if (dir) {
                header.linkFlag = LF_DIR
                if (header.name[header.name.length - 1] != '/') {
                    header.name += "/"
                }
                header.size = 0
            } else {
                header.linkFlag = LF_NORMAL
                header.size = size
            }

            header.modTime = modTime
            header.checkSum = 0
            header.devMajor = 0
            header.devMinor = 0

            return header
        }
    }
}
