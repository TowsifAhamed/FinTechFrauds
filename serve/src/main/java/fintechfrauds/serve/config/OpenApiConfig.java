package fintechfrauds.serve.config;

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
                .title("FinTechFrauds API")
                .version("v1")
                .description(
                    """
                        Scoring, rules, and moderated fraud ledger.
                        Protected endpoints require HMAC headers:
                        X-Api-Key, X-Timestamp, X-Nonce, X-Idempotency-Key (POST), X-Signature
                        (base64 HMAC-SHA256 of the canonical request string).
                        """
                        .stripIndent())
                .contact(new Contact().name("FinTechFrauds Team")))
        .addSecurityItem(new SecurityRequirement().addList("apiKey"))
        .components(
            new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes(
                    "apiKey",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Api-Key")
                        .description(
                            "Use together with X-Timestamp, X-Nonce, X-Signature, and X-Idempotency-Key (POST).")));
  }
}
