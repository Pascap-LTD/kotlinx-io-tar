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

import com.goncalossilva.resources.Resource
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.buffered
import org.intellij.lang.annotations.Language
import kotlin.test.*

class TarSourceTest {

    private fun getTestStream(@Language("file-reference") archive: String): TarSource {
        val resource = Resource(archive)
        val buffer = Buffer()
        buffer.write(resource.readBytes())
        buffer.flush()
        val source = buffer as Source
        return source.buffered().tar()
    }

    @Test
    fun testCompress197() {
        getTestStream("COMPRESS-197.tar")
            .use { tar ->
                var entry = tar.nextEntry
                assertNotNull(entry)
                while (entry != null) {

                    assertTrue(entry.header.isTypeFlagUStar())
                    entry = tar.nextEntry
                }
            }
    }

    @Test
    fun testSurvivesBlankLinesInPaxHeader() {
        getTestStream("COMPRESS-355.tar").use { tar ->
            val entry = tar.nextEntry
            assertNotNull(entry)
            assertEquals("package/package.json", entry.name)
            assertEquals(TarHeader.LF_NORMAL, entry.header.linkFlag)
            assertNull(tar.nextEntry)
        }
    }

    @Test
    fun testSurvivesPaxHeaderWithNameEndingInSlash() {
        getTestStream("COMPRESS-356.tar").use { tar ->
            val entry = tar.nextEntry
            assertNotNull(entry)
            assertEquals("package/package.json", entry.name)
            assertEquals(TarHeader.LF_NORMAL, entry.header.linkFlag)
            assertNull(tar.nextEntry)
        }
    }

//    @Test
//    fun testThrowExceptionWithNullEntry() {
//        getTestStream("COMPRESS-554-fail.tar").use { tarSource ->
//            tarSource.nextEntry?.
//        }
//    }

    @Test
    fun testWorkaroundForBrokenTimeHeader() {
        getTestStream("simple-aix-native-tar.tar").use { tar ->
            var tae = tar.nextEntry
            tae = tar.nextEntry
            assertEquals("sample/link-to-txt-file.lnk", tae?.name)
            assertEquals(TarHeader.LF_SYMLINK, tae?.header?.linkFlag)
            assertEquals(0, tae?.header?.modTime)
//            assertTrue(tae.isSymbolicLink())
//            assertTrue(tae.isCheckSumOK())
        }
    }
}