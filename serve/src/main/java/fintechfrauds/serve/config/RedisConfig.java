package fintechfrauds.serve.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisConfig {

  @Bean(destroyMethod = "close")
  public JedisPooled jedisPooled(
      @Value("${redis.host:localhost}") String host,
      @Value("${redis.port:6379}") int port,
      @Value("${redis.timeoutMillis:2000}") int timeoutMillis) {
    JedisClientConfig config =
        DefaultJedisClientConfig.builder()
            .connectionTimeoutMillis(timeoutMillis)
            .socketTimeoutMillis(timeoutMillis)
            .build();
    return new JedisPooled(new HostAndPort(host, port), config);
  }
}
