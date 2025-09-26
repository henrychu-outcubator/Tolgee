package io.tolgee.configuration

import io.tolgee.component.logging.ApiRequestLoggingInterceptor
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class RestTemplateConfiguration(
  private val tolgeeProperties: TolgeeProperties,
  private val apiRequestLoggingInterceptor: ApiRequestLoggingInterceptor,
) {
  @Bean
  @Lazy
  @Primary
  fun restTemplate(): RestTemplate {
    val restTemplate = RestTemplate(
      HttpComponentsClientHttpRequestFactory().apply {
        this.httpClient = HttpClientBuilder.create().disableCookieManagement().useSystemProperties().build()
      },
    ).removeXmlConverter()

    // Add logging interceptor if API logging is enabled
    if (tolgeeProperties.apiLogging.enabled) {
      restTemplate.interceptors.add(apiRequestLoggingInterceptor)
    }

    return restTemplate
  }

  private fun RestTemplate.removeXmlConverter(): RestTemplate {
    messageConverters.removeIf { it is MappingJackson2XmlHttpMessageConverter }
    return this
  }

  @Bean(name = ["webhookRestTemplate"])
  fun webhookRestTemplate(): RestTemplate {
    val restTemplate = RestTemplate(getClientHttpRequestFactory()).removeXmlConverter()

    // Add logging interceptor for webhooks if API logging is enabled
    if (tolgeeProperties.apiLogging.enabled) {
      restTemplate.interceptors.add(apiRequestLoggingInterceptor)
    }

    return restTemplate
  }

  private fun getClientHttpRequestFactory(): SimpleClientHttpRequestFactory {
    val clientHttpRequestFactory = SimpleClientHttpRequestFactory()
    clientHttpRequestFactory.setConnectTimeout(2000)
    clientHttpRequestFactory.setReadTimeout(2000)
    return clientHttpRequestFactory
  }
}
