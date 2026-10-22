package com.example.sereneread.data.remote.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer

// Membungkus RequestBody untuk melaporkan berapa persen yang sudah terkirim ke server.
class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (percent: Int) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    // One-shot: HttpLoggingInterceptor (level BODY) tidak akan membaca isi body untuk log.
    // Tanpa ini, isi PDF ditulis dua kali (sekali ke log) dan progres akan lompat ke 100% lalu ulang dari 0.
    override fun isOneShot(): Boolean = true

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        var lastPercent = -1

        val countingSink = object : ForwardingSink(sink) {
            var written = 0L

            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                written += byteCount
                if (total > 0) {
                    val percent = (written * 100 / total).toInt()
                    // Hanya kabari saat persentase berubah, bukan setiap potongan byte
                    if (percent != lastPercent) {
                        lastPercent = percent
                        onProgress(percent)
                    }
                }
            }
        }.buffer()

        delegate.writeTo(countingSink)
        countingSink.flush()
    }
}
