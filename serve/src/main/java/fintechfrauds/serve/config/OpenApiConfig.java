package fintechfrauds.serve.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI fintechFraudsOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("FinTechFrauds APIs")
                .description(
                    "APIs are protected with HMAC headers (X-Api-Key, X-Timestamp, X-Nonce, X-Idempotency-Key, X-Signature).")
                .contact(new Contact().name("Risk Engineering"))
                .version("1.0.0"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "hmac",
                    new SecurityScheme()
                        .name("X-Signature")
                        .scheme("HMAC-SHA256")
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)))
        .addSecurityItem(new SecurityRequirement().addList("hmac"));
  }
}
