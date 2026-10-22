package com.example.sereneread.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorsTest {

    private fun httpError(code: Int, body: String) = HttpException(
        Response.error<Any>(code, body.toResponseBody("application/json".toMediaType()))
    )

    @Test
    fun `shows the message sent by the Go backend`() {
        val error = httpError(400, """{"status":false,"message":"Judul buku tidak boleh kosong"}""")
        assertEquals("Judul buku tidak boleh kosong", error.toUserMessage())
    }

    @Test
    fun `falls back to status code when body is not JSON`() {
        assertEquals("Server error (502)", httpError(502, "<html>Bad gateway</html>").toUserMessage())
    }

    @Test
    fun `falls back to status code when message is blank`() {
        assertEquals("Server error (500)", httpError(500, """{"status":false,"message":""}""").toUserMessage())
    }

    @Test
    fun `network failure gets a connection message`() {
        assertEquals(
            "No internet connection. Check your connection and try again.",
            IOException("timeout").toUserMessage()
        )
    }
}
