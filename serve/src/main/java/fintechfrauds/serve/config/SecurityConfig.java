package fintechfrauds.serve.config;

import fintechfrauds.serve.security.ApiAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class SecurityConfig {

  @Bean
  public FilterRegistrationBean<ApiAuthFilter> apiAuthFilterRegistration(ApiAuthFilter filter) {
    FilterRegistrationBean<ApiAuthFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    return registration;
  }
}
