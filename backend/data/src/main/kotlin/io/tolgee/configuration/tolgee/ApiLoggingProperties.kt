package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.api-logging")
@DocProperty(
  description = "Configuration for API request logging to external systems",
  displayName = "API Logging"
)
class ApiLoggingProperties {
  @DocProperty(description = "Enable/disable API request logging")
  var enabled: Boolean = true

  @DocProperty(description = "Log level for API requests (ERROR, WARN, INFO, DEBUG)")
  var level: LogLevel = LogLevel.INFO

  @DocProperty(description = "Include request/response bodies in logs")
  var includePayload: Boolean = true

  @DocProperty(description = "Include request/response headers in logs")
  var includeHeaders: Boolean = true

  @DocProperty(description = "Maximum length of logged request/response body")
  var maxPayloadLength: Int = 1000

  @DocProperty(description = "Include timing information (duration) in logs")
  var includeTiming: Boolean = true

  @DocProperty(description = "Enable detailed logging for specific API types")
  var detailedLogging: DetailedLoggingConfig = DetailedLoggingConfig()

  @DocProperty(description = "Log quota and rate limit information")
  var includeQuotaInfo: Boolean = true

  @DocProperty(description = "Sanitize sensitive data in logs")
  var sanitizeSensitiveData: Boolean = true

  enum class LogLevel {
    ERROR,
    WARN,
    INFO,
    DEBUG
  }

  class DetailedLoggingConfig {
    @DocProperty(description = "Enable detailed logging for machine translation APIs")
    var machineTranslation: Boolean = true

    @DocProperty(description = "Enable detailed logging for OAuth authentication")
    var authentication: Boolean = true

    @DocProperty(description = "Enable detailed logging for webhook calls")
    var webhooks: Boolean = true

    @DocProperty(description = "Enable detailed logging for content delivery/CDN")
    var contentDelivery: Boolean = false

    @DocProperty(description = "Enable detailed logging for telemetry services")
    var telemetry: Boolean = false

    @DocProperty(description = "Enable detailed logging for LLM providers")
    var llmProviders: Boolean = true

    @DocProperty(description = "Enable detailed logging for file storage operations")
    var fileStorage: Boolean = false

    @DocProperty(description = "Enable detailed logging for email services")
    var emailServices: Boolean = false
  }
}
