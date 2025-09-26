package io.tolgee.component.logging

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ExternalApiLogger {

    companion object {
        private val logger = LoggerFactory.getLogger(ExternalApiLogger::class.java)
        private const val MDC_API_TYPE = "api_type"
        private const val MDC_API_PROVIDER = "api_provider"
        private const val MDC_REQUEST_ID = "request_id"
        private const val MDC_USER_ID = "user_id"
        private const val MDC_PROJECT_ID = "project_id"
    }

    enum class ApiType {
        MACHINE_TRANSLATION,
        OAUTH_AUTHENTICATION,
        WEBHOOK,
        CONTENT_DELIVERY,
        TELEMETRY,
        RECAPTCHA,
        LLM_PROVIDER,
        CACHE_PURGING,
        EMAIL_SERVICE,
        FILE_STORAGE
    }

    fun logApiCall(
        apiType: ApiType,
        provider: String,
        operation: String,
        url: String? = null,
        userId: Long? = null,
        projectId: Long? = null,
        additionalData: Map<String, Any> = emptyMap(),
        block: () -> Unit
    ) {
        val requestId = generateRequestId()
        val startTime = Instant.now()

        try {
            // Set MDC context
            MDC.put(MDC_API_TYPE, apiType.name)
            MDC.put(MDC_API_PROVIDER, provider)
            MDC.put(MDC_REQUEST_ID, requestId)
            userId?.let { MDC.put(MDC_USER_ID, it.toString()) }
            projectId?.let { MDC.put(MDC_PROJECT_ID, it.toString()) }

            logger.info(
                "EXTERNAL_API_START [{}] {} | Provider: {} | Operation: {} | URL: {} | User: {} | Project: {} | Data: {}",
                requestId, apiType.name, provider, operation, url, userId, projectId,
                additionalData.takeIf { it.isNotEmpty() } ?: "none"
            )

            block()

            val endTime = Instant.now()
            val duration = java.time.Duration.between(startTime, endTime).toMillis()

            logger.info(
                "EXTERNAL_API_SUCCESS [{}] {} | Provider: {} | Duration: {}ms",
                requestId,
              apiType.name,
              provider,
              duration
            )
        } catch (ex: Exception) {
            val endTime = Instant.now()
            val duration = java.time.Duration.between(startTime, endTime).toMillis()

            logger.error(
                "EXTERNAL_API_ERROR [{}] {} | Provider: {} | Duration: {}ms | Error: {} | Message: {}",
                requestId,
              apiType.name,
              provider,
              duration,
              ex.javaClass.simpleName,
              ex.message,
              ex
            )
            throw ex
        } finally {
            // Clean up MDC
            MDC.remove(MDC_API_TYPE)
            MDC.remove(MDC_API_PROVIDER)
            MDC.remove(MDC_REQUEST_ID)
            MDC.remove(MDC_USER_ID)
            MDC.remove(MDC_PROJECT_ID)
        }
    }

    fun <T> logApiCallWithResult(
        apiType: ApiType,
        provider: String,
        operation: String,
        url: String? = null,
        userId: Long? = null,
        projectId: Long? = null,
        additionalData: Map<String, Any> = emptyMap(),
        block: () -> T
    ): T {
        val requestId = generateRequestId()
        val startTime = Instant.now()

        return try {
            // Set MDC context
            MDC.put(MDC_API_TYPE, apiType.name)
            MDC.put(MDC_API_PROVIDER, provider)
            MDC.put(MDC_REQUEST_ID, requestId)
            userId?.let { MDC.put(MDC_USER_ID, it.toString()) }
            projectId?.let { MDC.put(MDC_PROJECT_ID, it.toString()) }

            logger.info(
                "EXTERNAL_API_START [{}] {} | Provider: {} | Operation: {} | URL: {} | User: {} | Project: {} | Data: {}",
                requestId, apiType.name, provider, operation, url, userId, projectId,
                additionalData.takeIf { it.isNotEmpty() } ?: "none"
            )

            val result = block()

            val endTime = Instant.now()
            val duration = java.time.Duration.between(startTime, endTime).toMillis()

            logger.info(
                "EXTERNAL_API_SUCCESS [{}] {} | Provider: {} | Duration: {}ms | Result: {}",
                requestId,
              apiType.name,
              provider,
              duration,
                if (result != null) "success" else "null"
            )

            result
        } catch (ex: Exception) {
            val endTime = Instant.now()
            val duration = java.time.Duration.between(startTime, endTime).toMillis()

            logger.error(
                "EXTERNAL_API_ERROR [{}] {} | Provider: {} | Duration: {}ms | Error: {} | Message: {}",
                requestId,
              apiType.name,
              provider,
              duration,
              ex.javaClass.simpleName,
              ex.message,
              ex
            )
            throw ex
        } finally {
            // Clean up MDC
            MDC.remove(MDC_API_TYPE)
            MDC.remove(MDC_API_PROVIDER)
            MDC.remove(MDC_REQUEST_ID)
            MDC.remove(MDC_USER_ID)
            MDC.remove(MDC_PROJECT_ID)
        }
    }

    fun logQuotaUsage(
        apiType: ApiType,
        provider: String,
        operation: String,
        charactersUsed: Long? = null,
        requestCount: Int = 1,
        remainingQuota: Long? = null,
        userId: Long? = null,
        projectId: Long? = null
    ) {
        logger.info(
            "API_QUOTA_USAGE {} | Provider: {} | Operation: {} | Characters: {} | Requests: {} | Remaining: {} | User: {} | Project: {}",
            apiType.name, provider, operation, charactersUsed ?: "N/A", requestCount,
            remainingQuota ?: "unknown", userId ?: "N/A", projectId ?: "N/A"
        )
    }

    fun logRateLimit(
        apiType: ApiType,
        provider: String,
        rateLimitRemaining: Int? = null,
        rateLimitReset: String? = null,
        retryAfter: Long? = null
    ) {
        logger.warn(
            "API_RATE_LIMIT {} | Provider: {} | Remaining: {} | Reset: {} | Retry After: {}s",
            apiType.name,
          provider,
          rateLimitRemaining ?: "unknown",
          rateLimitReset ?: "unknown",
              retryAfter ?: "unknown"
        )
    }

    private fun generateRequestId(): String {
        return System.currentTimeMillis().toString(36) + "-" +
               (Math.random() * 1000).toInt().toString(36)
    }
}
