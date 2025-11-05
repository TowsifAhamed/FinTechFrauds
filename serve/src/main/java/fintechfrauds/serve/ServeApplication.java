package fintechfrauds.serve;

import fintechfrauds.serve.config.FintechFraudsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "fintechfrauds")
@EnableConfigurationProperties(FintechFraudsProperties.class)
public class ServeApplication {
  public static void main(String[] args) {
    SpringApplication.run(ServeApplication.class, args);
  }
}
