package io.tolgee.component

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.logging.ExternalApiLogger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Instant

@Component
class HttpClient(
  private val restTemplate: RestTemplate,
  private val externalApiLogger: ExternalApiLogger,
) {
  private val logger = LoggerFactory.getLogger(HttpClient::class.java)

  fun <T> requestForJson(
    url: String,
    body: Any,
    method: HttpMethod,
    result: Class<T>,
    headers: HttpHeaders = HttpHeaders(),
    apiType: ExternalApiLogger.ApiType? = null,
    provider: String? = null,
    operation: String? = null,
    userId: Long? = null,
    projectId: Long? = null,
  ): T {
    return if (apiType != null && provider != null) {
      externalApiLogger.logApiCallWithResult(
        apiType = apiType,
        provider = provider,
        operation = operation ?: method.name(),
        url = url,
        userId = userId,
        projectId = projectId,
        additionalData = mapOf(
          "method" to method.name(),
          "bodyType" to body.javaClass.simpleName
        )
      ) {
        executeRequest(url, body, method, result, headers)
      }
    } else {
      // Fallback logging for requests without explicit API type
      logBasicRequest(url, method, body)
      executeRequest(url, body, method, result, headers)
    }
  }

  // Convenience methods for specific API types
  fun <T> requestForMachineTranslation(
    url: String,
    body: Any,
    method: HttpMethod,
    result: Class<T>,
    provider: String,
    operation: String = "translate",
    headers: HttpHeaders = HttpHeaders(),
    userId: Long? = null,
    projectId: Long? = null,
  ): T {
    return requestForJson(
      url = url,
      body = body,
      method = method,
      result = result,
      headers = headers,
      apiType = ExternalApiLogger.ApiType.MACHINE_TRANSLATION,
      provider = provider,
      operation = operation,
      userId = userId,
      projectId = projectId
    )
  }

  fun <T> requestForAuth(
    url: String,
    body: Any,
    method: HttpMethod,
    result: Class<T>,
    provider: String,
    operation: String = "authenticate",
    headers: HttpHeaders = HttpHeaders(),
    userId: Long? = null,
  ): T {
    return requestForJson(
      url = url,
      body = body,
      method = method,
      result = result,
      headers = headers,
      apiType = ExternalApiLogger.ApiType.OAUTH_AUTHENTICATION,
      provider = provider,
      operation = operation,
      userId = userId
    )
  }

  fun <T> requestForWebhook(
    url: String,
    body: Any,
    method: HttpMethod,
    result: Class<T>,
    operation: String = "webhook_call",
    headers: HttpHeaders = HttpHeaders(),
    userId: Long? = null,
    projectId: Long? = null,
  ): T {
    return requestForJson(
      url = url,
      body = body,
      method = method,
      result = result,
      headers = headers,
      apiType = ExternalApiLogger.ApiType.WEBHOOK,
      provider = extractProviderFromUrl(url),
      operation = operation,
      userId = userId,
      projectId = projectId
    )
  }

  fun <T> requestForLlm(
    url: String,
    body: Any,
    method: HttpMethod,
    result: Class<T>,
    provider: String,
    operation: String = "generate",
    headers: HttpHeaders = HttpHeaders(),
    userId: Long? = null,
    projectId: Long? = null,
  ): T {
    return requestForJson(
      url = url,
      body = body,
      method = method,
      result = result,
      headers = headers,
      apiType = ExternalApiLogger.ApiType.LLM_PROVIDER,
      provider = provider,
      operation = operation,
      userId = userId,
      projectId = projectId
    )
  }

  private fun <T> executeRequest(
    url: String,
    body: Any,
    method: HttpMethod,
    result: Class<T>,
    headers: HttpHeaders,
  ): T {
    val bodyJson = jacksonObjectMapper().writeValueAsString(body)
    headers.apply {
      contentType = MediaType.APPLICATION_JSON
    }

    val response = restTemplate.exchange(
      url,
      method,
      HttpEntity(bodyJson, headers),
      String::class.java,
    )

    if (result == Unit::class.java) {
      @Suppress("UNCHECKED_CAST")
      return Unit as T
    }

    return response.body.let { stringResponseBody ->
      jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(stringResponseBody, result)
    }
  }

  private fun logBasicRequest(url: String, method: HttpMethod, body: Any) {
    logger.info(
      "HTTP_REQUEST {} {} | Body: {} | Timestamp: {}",
      method.name(),
      sanitizeUrl(url),
      body.javaClass.simpleName,
      Instant.now()
    )
  }

  private fun sanitizeUrl(url: String): String {
    return url.replace(Regex("([?&])(api[_-]?key|token|auth[_-]?key|secret)=[^&]*"), "$1$2=***")
  }

  private fun extractProviderFromUrl(url: String): String {
    return try {
      val host = java.net.URL(url).host
      when {
        host.contains("googleapis") -> "Google"
        host.contains("deepl") -> "DeepL"
        host.contains("microsoft") -> "Microsoft"
        host.contains("amazonaws") -> "AWS"
        host.contains("azure") -> "Azure"
        host.contains("github") -> "GitHub"
        host.contains("slack") -> "Slack"
        else -> host.split(".").firstOrNull { it.length > 3 } ?: "Unknown"
      }
    } catch (e: Exception) {
      "Unknown"
    }
  }
}
