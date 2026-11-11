package com.financas.app.data.network

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object SafeHttp {

    const val USER_AGENT = "Finflow-App-Android"
    const val DEFAULT_MAX_BYTES = 512 * 1024

    val ALLOWED_API_HOSTS = setOf(
        "api.github.com",
        "query1.finance.yahoo.com"
    )

    fun isAllowedUrl(url: String, allowedHosts: Set<String> = ALLOWED_API_HOSTS): Boolean {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return false
        }
        val host = uri.host ?: return false
        return uri.scheme == "https" &&
            uri.port == -1 &&
            uri.rawUserInfo == null &&
            allowedHosts.any { it.equals(host, ignoreCase = true) }
    }

    fun getText(
        url: String,
        accept: String? = null,
        maxBytes: Int = DEFAULT_MAX_BYTES,
        allowedHosts: Set<String> = ALLOWED_API_HOSTS
    ): String {
        require(isAllowedUrl(url, allowedHosts)) { "Servidor não autorizado." }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 8000
            requestMethod = "GET"
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("User-Agent", USER_AGENT)
            accept?.let { setRequestProperty("Accept", it) }
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            if (connection.contentLengthLong > maxBytes) throw IOException("Resposta demasiado grande.")
            return connection.inputStream.use { input -> readLimited(input, maxBytes) }
        } finally {
            connection.disconnect()
        }
    }

    internal fun readLimited(input: java.io.InputStream, maxBytes: Int): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw IOException("Resposta demasiado grande.")
            out.write(buffer, 0, read)
        }
        return out.toString(Charsets.UTF_8.name())
    }
}
