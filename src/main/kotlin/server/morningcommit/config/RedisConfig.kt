package server.morningcommit.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class RedisConfig {

    companion object {
        const val ANALYTICS_DASHBOARD = "analytics-dashboard"
        const val POST_LISTING = "post-listing"
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val objectMapper = createRedisObjectMapper()

        val jsonSerializer = GenericJackson2JsonRedisSerializer(objectMapper)
        val stringSerializer = StringRedisSerializer()

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .disableCachingNullValues()
            .entryTtl(Duration.ofMinutes(30))

        val cacheConfigs = mapOf(
            ANALYTICS_DASHBOARD to defaultConfig.entryTtl(Duration.ofMinutes(10)),
            POST_LISTING to defaultConfig.entryTtl(Duration.ofMinutes(30)),
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build()
    }

    private fun createRedisObjectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())

            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

            val typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Any::class.java)
                .build()

            @Suppress("DEPRECATION")
            activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.EVERYTHING)
        }
    }
}
