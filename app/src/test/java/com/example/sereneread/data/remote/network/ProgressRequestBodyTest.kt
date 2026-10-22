package com.example.sereneread.data.remote.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRequestBodyTest {

    private val bytes = ByteArray(200_000) { (it % 251).toByte() }
    private val delegate = bytes.toRequestBody("application/pdf".toMediaType())

    @Test
    fun `writes the same bytes as the wrapped body`() {
        val body = ProgressRequestBody(delegate) {}
        val sink = Buffer()
        body.writeTo(sink)
        assertArrayEquals(bytes, sink.readByteArray())
    }

    @Test
    fun `reports increasing percentages ending at 100`() {
        val reported = mutableListOf<Int>()
        ProgressRequestBody(delegate) { reported += it }.writeTo(Buffer())

        assertTrue("should report progress", reported.isNotEmpty())
        assertEquals(100, reported.last())
        assertEquals("never goes backwards", reported.sorted(), reported)
        assertEquals("no duplicate reports", reported.distinct(), reported)
    }

    @Test
    fun `delegates content type and length`() {
        val body = ProgressRequestBody(delegate) {}
        assertEquals(delegate.contentType(), body.contentType())
        assertEquals(bytes.size.toLong(), body.contentLength())
    }

    @Test
    fun `is one shot so the logging interceptor does not read the PDF`() {
        assertTrue(ProgressRequestBody(delegate) {}.isOneShot())
    }
}
