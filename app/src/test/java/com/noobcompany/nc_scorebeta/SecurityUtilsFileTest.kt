package com.noobcompany.nc_scorebeta

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SecurityUtilsFileTest {

    @Test
    fun `isSafeFilePath allows file inside root`() {
        val tempDir = Files.createTempDirectory("test_root").toFile()
        val safeFile = File(tempDir, "safe.txt")
        assertTrue("File inside root should be safe", SecurityUtils.isSafeFilePath(safeFile, tempDir))
        tempDir.deleteRecursively()
    }

    @Test
    fun `isSafeFilePath blocks file outside root`() {
        val tempDir = Files.createTempDirectory("test_root").toFile()
        val outsideFile = File(tempDir.parentFile, "outside.txt")
        assertFalse("File outside root should be unsafe", SecurityUtils.isSafeFilePath(outsideFile, tempDir))
        tempDir.deleteRecursively()
    }

    @Test
    fun `isSafeFilePath blocks traversal attack`() {
        val tempDir = Files.createTempDirectory("test_root").toFile()
        // Create a path that looks like it is inside but traverses out
        val traversalFile = File(tempDir, "../outside.txt")
        assertFalse("Traversal path should be unsafe", SecurityUtils.isSafeFilePath(traversalFile, tempDir))
        tempDir.deleteRecursively()
    }

    @Test
    fun `isSafeFilePath prevents partial directory match`() {
         // This is the /data/foo vs /data/foobar case
         val parent = Files.createTempDirectory("parent").toFile()
         val root = File(parent, "foo")
         root.mkdirs()

         val sibling = File(parent, "foobar")
         sibling.mkdirs()
         val maliciousFile = File(sibling, "hack.txt")

         assertFalse("Partial directory match should be unsafe", SecurityUtils.isSafeFilePath(maliciousFile, root))
         parent.deleteRecursively()
    }
}
