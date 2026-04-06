package com.team.hubservice.global.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.time.Duration;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@EnableCaching
@Configuration
public class CacheConfig {

    public static final String HUB_CACHE = "hubs";
    public static final String HUB_ROUTE_CACHE = "hubRoutes";
    public static final String OPTIMAL_ROUTE_CACHE = "optimalRoutes";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Object.class)
            .build();

        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new ParameterNamesModule())
            .activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
            );

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
            .prefixCacheNameWith("hub-service::")
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(objectMapper)
            ));

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            HUB_CACHE, baseConfig.entryTtl(Duration.ofDays(7)),
            HUB_ROUTE_CACHE, baseConfig.entryTtl(Duration.ofDays(1)),
            OPTIMAL_ROUTE_CACHE, baseConfig.entryTtl(Duration.ofHours(1))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(baseConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
