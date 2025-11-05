package fintechfrauds.serve.config;

import fintechfrauds.serve.scoring.DummyScorer;
import fintechfrauds.serve.scoring.Scorer;
import fintechfrauds.serve.scoring.XgbScorer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class ScorerConfig {

  private static final Logger log = LoggerFactory.getLogger(ScorerConfig.class);

  @Bean
  @org.springframework.context.annotation.Primary
  public Scorer scorer(FintechFraudsProperties properties, DummyScorer dummyScorer) {
    if (!"xgb".equalsIgnoreCase(properties.getModel().getType())) {
      return dummyScorer;
    }

    Path modelPath = resolveModel(properties.getModel());
    if (modelPath == null) {
      log.warn("xgb_model_missing_fallback_dummy");
      return dummyScorer;
    }

    try {
      XgbScorer scorer = XgbScorer.fromPath(modelPath);
      log.info("xgb_model_loaded path={} size={}", modelPath, Files.size(modelPath));
      return scorer;
    } catch (IOException | XGBoostError e) {
      log.warn("xgb_model_load_failed_fallback", e);
      return dummyScorer;
    }
  }

  private Path resolveModel(FintechFraudsProperties.Model model) {
    if (model.getUri() != null) {
      try {
        return downloadModel(model.getUri(), model.getSha256());
      } catch (IOException e) {
        log.warn("model_download_failed", e);
      }
    }
    Path resourcePath = Paths.get("serve/src/main/resources").resolve(model.getResourcePath());
    if (Files.exists(resourcePath)) {
      return resourcePath;
    }
    Path runtimePath = Paths.get(model.getResourcePath());
    if (Files.exists(runtimePath)) {
      return runtimePath;
    }
    ClassPathResource resource = new ClassPathResource(model.getResourcePath());
    if (resource.exists()) {
      try {
        Path tempFile = Files.createTempFile("fintechfrauds-model", ".xgb");
        try (InputStream inputStream = resource.getInputStream()) {
          Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
      } catch (IOException e) {
        log.warn("model_classpath_extract_failed", e);
      }
    }
    return null;
  }

  private Path downloadModel(URI uri, String sha256) throws IOException {
    Path tempFile = Files.createTempFile("fintechfrauds-model", ".xgb");
    try (InputStream input = uri.toURL().openStream()) {
      Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    if (sha256 != null) {
      verifyChecksum(tempFile, sha256);
    }
    return tempFile;
  }

  private void verifyChecksum(Path file, String expected) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream in = Files.newInputStream(file);
          DigestInputStream dis = new DigestInputStream(in, digest)) {
        dis.transferTo(OutputStream.nullOutputStream());
      }
      String actual = HexFormat.of().formatHex(digest.digest());
      if (!actual.equalsIgnoreCase(expected)) {
        throw new IOException("Checksum mismatch for model file");
      }
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("SHA-256 algorithm not available", e);
    }
  }
}
