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

import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.readTo

class TarSource(private val source: Source) : AutoCloseable {
    private var currentEntry: TarEntry? = null
    private var currentFileSize: Long = 0

    /**
     * Returns the current offset (in bytes) from the beginning of the stream.
     * This can be used to find out at which point in a tar file an entry's content begins, for instance.
     */
    var currentOffset: Long = 0
        private set

    override fun close() = source.close()

    fun currentFile(): Source {
        val buffer = Buffer()
        source.readTo(buffer, currentEntry?.size ?: 0L)
        return buffer
    }

    val nextEntry: TarEntry?
        /**
         * Returns the next entry in the tar file
         *
         * @return TarEntry
         * @throws IOException
         */
        get() {
            closeCurrentEntry()

            val header = ByteArray(TarConstants.HEADER_BLOCK)
            source.readTo(header)

            var eof = true
            for (b in header) {
                if (b.toInt() != 0) {
                    eof = false
                    break
                }
            }

            if (!eof) {
                currentEntry = TarEntry(header)
            } else {
                source.skip(TarConstants.HEADER_BLOCK.toLong())
            }

            return currentEntry
        }

    /**
     * Closes the current tar entry
     */
    private fun closeCurrentEntry() {
        if (currentEntry != null) {
            currentOffset += currentEntry?.size ?: 0L
            currentEntry = null
            currentFileSize = 0L
            skipPad()
        }
    }

    /**
     * Skips the pad at the end of each tar entry file content
     */
    private fun skipPad() {
        if (currentOffset > 0) {
            val extra = currentOffset % TarConstants.DATA_BLOCK

            if (extra > 0) {
                val n = TarConstants.DATA_BLOCK - extra
                source.skip(n)
                currentOffset += n
            }
        }
    }

}

inline fun Source.tar() = TarSource(this)
