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

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class TarSink(private val sink: Sink) : AutoCloseable {
    private var bytesWritten: Long = 0
    private var currentFileSize: Long = 0
    private var currentEntry: TarEntry? = null

    /**
     * Appends the EOF record and closes the stream
     */
    override fun close() {
        closeCurrentEntry()
        write(ByteArray(TarConstants.EOF_BLOCK))
        sink.close()
    }

    fun flush() = sink.flush()

    /**
     * Checks if the bytes being written exceed the current entry size.
     */
    fun write(b: ByteArray, off: Int = 0, len: Int = b.size) {
        if (currentEntry != null && !currentEntry!!.isDirectory) {
            if (currentEntry!!.size < currentFileSize + len) {
                throw IOException(
                    ("The current entry[${currentEntry!!.name}] size[${currentEntry!!.size}] is smaller than the bytes[${currentFileSize + len}] being written.")
                )
            }
        }

        sink.write(b, off, len)

        bytesWritten += len.toLong()

        if (currentEntry != null) {
            currentFileSize += len.toLong()
        }
    }

    fun write(path: Path) {
        val written = SystemFileSystem.source(path).buffered()
            .transferTo(sink)
        bytesWritten += written
        if (currentEntry != null) {
            currentFileSize += written
        }
        sink.flush()
    }

    /**
     * Writes the next tar entry header on the stream
     */
    fun putNextEntry(entry: TarEntry) {
        closeCurrentEntry()

        val header = ByteArray(TarConstants.HEADER_BLOCK)
        entry.writeEntryHeader(header)

        write(header)

        currentEntry = entry
    }

    /**
     * Closes the current tar entry
     */
    private fun closeCurrentEntry() {
        currentEntry?.run {
            if (size > currentFileSize) {
                throw IOException(("The current entry[$name] of size[$size] has not been fully written."))
            }

            currentEntry = null
            currentFileSize = 0

            pad()
        }
    }

    /**
     * Pads the last content block
     */
    private fun pad() {
        if (bytesWritten > 0) {
            val extra = (bytesWritten % TarConstants.DATA_BLOCK).toInt()

            if (extra > 0) {
                write(ByteArray(TarConstants.DATA_BLOCK - extra))
            }
        }
    }
}

inline fun Sink.tar() = TarSink(this)
