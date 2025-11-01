package fintechfrauds.serve.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

  @Bean
  public JedisPool jedisPool(Environment environment) {
    String host = environment.getProperty("redis.host", "127.0.0.1");
    int port = Integer.parseInt(environment.getProperty("redis.port", "6379"));
    int timeout = Integer.parseInt(environment.getProperty("redis.timeoutMillis", "2000"));

    JedisPoolConfig config = new JedisPoolConfig();
    config.setMaxTotal(64);
    config.setMaxIdle(32);
    config.setMinIdle(2);

    return new JedisPool(config, host, port, timeout);
  }
}
