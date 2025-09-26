package io.tolgee.component.logging

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.Instant

@Component
class ApiRequestLoggingInterceptor : ClientHttpRequestInterceptor {

    private val logger = LoggerFactory.getLogger(ApiRequestLoggingInterceptor::class.java)
    private val sensitiveHeaders = setOf(
        "authorization", "auth-key", "x-api-key", "api-key", "token",
        "x-auth-token", "x-access-token", "bearer", "cookie", "set-cookie"
    )
    private val sensitiveBodyKeywords = setOf(
        "password",
      "secret",
      "key",
      "token",
      "auth",
      "credential"
    )

    @Throws(IOException::class)
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val startTime = Instant.now()
        val requestId = generateRequestId()

        logRequest(request, body, requestId)

        return try {
            val response = execution.execute(request, body)
            val endTime = Instant.now()
            val duration = java.time.Duration.between(startTime, endTime).toMillis()

            logResponse(response, requestId, duration)
            response
        } catch (ex: Exception) {
            val endTime = Instant.now()
            val duration = java.time.Duration.between(startTime, endTime).toMillis()

            logError(request, ex, requestId, duration)
            throw ex
        }
    }

    private fun logRequest(request: HttpRequest, body: ByteArray, requestId: String) {
        if (!logger.isInfoEnabled) return

        val method = request.method
        val url = sanitizeUrl(request.uri.toString())
        val headers = sanitizeHeaders(request.headers.toSingleValueMap())
        val bodyString = sanitizeBody(String(body, StandardCharsets.UTF_8))

        logger.info(
            "API_REQUEST [{}] {} {} | Headers: {} | Body: {}",
            requestId,
          method,
          url,
          headers,
            if (bodyString.length > 1000) "${bodyString.take(1000)}..." else bodyString
        )
    }

    private fun logResponse(response: ClientHttpResponse, requestId: String, duration: Long) {
        if (!logger.isInfoEnabled) return

        val statusCode = response.statusCode.value()
        val reasonPhrase = response.statusText
        val headers = sanitizeHeaders(response.headers.toSingleValueMap())

        try {
            val responseBody = StreamUtils.copyToString(response.body, StandardCharsets.UTF_8)
            val sanitizedBody = sanitizeBody(responseBody)

            logger.info(
                "API_RESPONSE [{}] {} {} | Duration: {}ms | Headers: {} | Body: {}",
                requestId,
              statusCode,
              reasonPhrase,
              duration,
              headers,
                if (sanitizedBody.length > 1000) "${sanitizedBody.take(1000)}..." else sanitizedBody
            )
        } catch (ex: Exception) {
            logger.warn(
              "API_RESPONSE [{}] {} {} | Duration: {}ms | Headers: {} | Body: <failed to read>",
                requestId,
              statusCode,
              reasonPhrase,
              duration,
              headers
            )
        }
    }

    private fun logError(request: HttpRequest, ex: Exception, requestId: String, duration: Long) {
        val method = request.method
        val url = sanitizeUrl(request.uri.toString())

        logger.error(
            "API_REQUEST_ERROR [{}] {} {} | Duration: {}ms | Error: {} | Message: {}",
            requestId,
          method,
          url,
          duration,
          ex.javaClass.simpleName,
          ex.message
        )
    }

    private fun sanitizeUrl(url: String): String {
        // Remove sensitive query parameters
        return url.replace(Regex("([?&])(api[_-]?key|token|auth[_-]?key|secret)=[^&]*"), "$1$2=***")
    }

    private fun sanitizeHeaders(headers: Map<String, String>): Map<String, String> {
        return headers.mapValues { (key, value) ->
            if (sensitiveHeaders.any { key.lowercase().contains(it) }) {
                "***"
            } else {
                value
            }
        }
    }

    private fun sanitizeBody(body: String): String {
        if (body.isBlank()) return body

        var sanitized = body
        sensitiveBodyKeywords.forEach { keyword ->
            // Replace sensitive values in JSON/form data
            sanitized = sanitized.replace(
                Regex("([\"']?$keyword[\"']?\\s*[:=]\\s*[\"']?)([^\"',}&\\s]+)([\"']?)", RegexOption.IGNORE_CASE),
                "$1***$3"
            )
        }
        return sanitized
    }

    private fun generateRequestId(): String {
        return System.currentTimeMillis().toString(36) + "-" +
               (Math.random() * 1000).toInt().toString(36)
    }
}
