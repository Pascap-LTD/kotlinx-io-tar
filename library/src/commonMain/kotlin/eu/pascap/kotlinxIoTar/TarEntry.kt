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

import eu.pascap.kotlinxIoTar.Octal.getCheckSumOctalBytes
import eu.pascap.kotlinxIoTar.Octal.getLongOctalBytes
import eu.pascap.kotlinxIoTar.Octal.getOctalBytes
import eu.pascap.kotlinxIoTar.Octal.parseOctal
import eu.pascap.kotlinxIoTar.TarHeader.Companion.createHeader
import eu.pascap.kotlinxIoTar.TarHeader.Companion.getNameBytes
import eu.pascap.kotlinxIoTar.TarHeader.Companion.parseName
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class TarEntry {
    var file: Path?
        private set
    var header: TarHeader
        private set

    private constructor() {
        this.file = null
        header = TarHeader()
    }

    constructor(file: Path, entryName: String) : this() {
        this.file = file
        this.extractTarHeader(entryName)
    }

    constructor(headerBuf: ByteArray) : this() {
        this.parseTarHeader(headerBuf)
    }

    /**
     * Constructor to create an entry from an existing TarHeader object.
     *
     * This method is useful to add new entries programmatically (e.g. for
     * adding files or directories that do not exist in the file system).
     */
    constructor(header: TarHeader) {
        this.file = null
        this.header = header
    }

    override fun equals(other: Any?): Boolean {
        return other is TarEntry && header.name == other.header.name
    }

    override fun hashCode(): Int {
        return header.name.hashCode()
    }

    fun isDescendent(desc: TarEntry): Boolean {
        return desc.header.name.startsWith(header.name)
    }

    var name: String
        get() {
            var name = header.name
            if (header.namePrefix != "") {
                name = header.namePrefix + "/" + name
            }

            return name
        }
        set(name) {
            header.name = name
        }

    var userId: Int
        get() = header.userId
        set(userId) {
            header.userId = userId
        }

    var groupId: Int
        get() = header.groupId
        set(groupId) {
            header.groupId = groupId
        }

    var userName: String
        get() = header.userName
        set(userName) {
            header.userName = userName
        }

    var groupName: String
        get() = header.groupName
        set(groupName) {
            header.groupName = groupName
        }

    fun setIds(userId: Int, groupId: Int) {
        this.userId = userId
        this.groupId = groupId
    }

    fun setModTime(time: Long) {
        header.modTime = time / 1000
    }

    var size: Long
        get() = header.size
        set(size) {
            header.size = size
        }

    val isDirectory: Boolean
        get() {
            file?.let { file ->
                return SystemFileSystem.metadataOrNull(file)?.isDirectory == true
            }

            if (header.linkFlag == TarHeader.LF_DIR) return true

            if (header.name.endsWith("/")) return true

            return false
        }

    /**
     * Extract header from File
     */
    fun extractTarHeader(entryName: String) {
        file?.let { file ->
            val metadata = SystemFileSystem.metadataOrNull(file)
            val permissions = StandardFilePermission.READ.mode // okio has no permissions api so just assume READ access by default //permissions(metadata)
            header = createHeader(entryName, metadata?.size ?: 0, 0, metadata?.isDirectory == true, permissions)
        } ?: throw Exception("File is null")
    }

    /**
     * Calculate checksum
     */
    fun computeCheckSum(buf: ByteArray): Long {
        var sum: Long = 0

        for (i in buf.indices) {
            sum += (255 and buf[i].toInt()).toLong()
        }

        return sum
    }

    /**
     * Writes the header to the byte buffer
     */
    fun writeEntryHeader(outbuf: ByteArray) {
        var offset = 0

        offset = getNameBytes(header.name, outbuf, offset, TarHeader.NAME_LEN)
        offset = getOctalBytes(header.mode.toLong(), outbuf, offset, TarHeader.MODE_LEN)
        offset = getOctalBytes(header.userId.toLong(), outbuf, offset, TarHeader.UID_LEN)
        offset = getOctalBytes(header.groupId.toLong(), outbuf, offset, TarHeader.GID_LEN)

        val size = header.size

        offset = getLongOctalBytes(size, outbuf, offset, TarHeader.SIZE_LEN)
        offset = getLongOctalBytes(header.modTime, outbuf, offset, TarHeader.MOD_TIME_LEN)

        val csOffset = offset
        for (c in 0..<TarHeader.CHK_SUM_LEN) outbuf[offset++] = ' '.code.toByte()

        outbuf[offset++] = header.linkFlag

        offset = getNameBytes(header.linkName, outbuf, offset, TarHeader.NAME_LEN)
        offset = getNameBytes(header.magic, outbuf, offset, TarHeader.USTAR_MAGIC_LEN)
        offset = getNameBytes(header.userName, outbuf, offset, TarHeader.USTAR_USER_NAME_LEN)
        offset = getNameBytes(header.groupName, outbuf, offset, TarHeader.USTAR_GROUP_NAME_LEN)
        offset = getOctalBytes(header.devMajor.toLong(), outbuf, offset, TarHeader.USTAR_DEV_LEN)
        offset = getOctalBytes(header.devMinor.toLong(), outbuf, offset, TarHeader.USTAR_DEV_LEN)
        offset = getNameBytes(header.namePrefix, outbuf, offset, TarHeader.USTAR_FILENAME_PREFIX)

        while (offset < outbuf.size) {
            outbuf[offset++] = 0
        }

        val checkSum = this.computeCheckSum(outbuf)

        getCheckSumOctalBytes(checkSum, outbuf, csOffset, TarHeader.CHK_SUM_LEN)
    }

    /**
     * Parses the tar header to the byte buffer
     */
    fun parseTarHeader(bh: ByteArray) {
        var offset = 0
        header.name = parseName(bh, offset, TarHeader.NAME_LEN)
        offset += TarHeader.NAME_LEN


        header.mode = parseOctal(bh, offset, TarHeader.MODE_LEN).toInt()
        offset += TarHeader.MODE_LEN

        header.userId = parseOctal(bh, offset, TarHeader.UID_LEN).toInt()
        offset += TarHeader.UID_LEN

        header.groupId = parseOctal(bh, offset, TarHeader.GID_LEN).toInt()
        offset += TarHeader.GID_LEN

        header.size = parseOctal(bh, offset, TarHeader.SIZE_LEN)
        offset += TarHeader.SIZE_LEN

        header.modTime = parseOctal(bh, offset, TarHeader.MOD_TIME_LEN)
        offset += TarHeader.MOD_TIME_LEN

        header.checkSum = parseOctal(bh, offset, TarHeader.CHK_SUM_LEN).toInt()
        offset += TarHeader.CHK_SUM_LEN

        header.linkFlag = bh[offset++]

        header.linkName = parseName(bh, offset, TarHeader.NAME_LEN)
        offset += TarHeader.NAME_LEN

        header.magic = parseName(bh, offset, TarHeader.USTAR_MAGIC_LEN)
        offset += TarHeader.USTAR_MAGIC_LEN

        header.userName = parseName(bh, offset, TarHeader.USTAR_USER_NAME_LEN)
        offset += TarHeader.USTAR_USER_NAME_LEN

        header.groupName = parseName(bh, offset, TarHeader.USTAR_GROUP_NAME_LEN)
        offset += TarHeader.USTAR_GROUP_NAME_LEN

        header.devMajor = parseOctal(bh, offset, TarHeader.USTAR_DEV_LEN).toInt()
        offset += TarHeader.USTAR_DEV_LEN

        header.devMinor = parseOctal(bh, offset, TarHeader.USTAR_DEV_LEN).toInt()
        offset += TarHeader.USTAR_DEV_LEN

        header.namePrefix = parseName(bh, offset, TarHeader.USTAR_FILENAME_PREFIX)
    }

    private enum class StandardFilePermission(val mode: Int) {
        EXECUTE(72), WRITE(144), READ(288)
    }
}
