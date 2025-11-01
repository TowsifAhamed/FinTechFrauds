package fintechfrauds.serve.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class BackfillFs {

  private BackfillFs() {}

  public static void main(String[] args) throws Exception {
    Map<String, String> arguments = parseArgs(args);
    String host = arguments.getOrDefault("host", "127.0.0.1");
    int port = Integer.parseInt(arguments.getOrDefault("port", "6379"));

    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), host, port, 2000)) {
      if (arguments.containsKey("csv")) {
        seedFromCsv(pool, Path.of(arguments.get("csv")));
      } else {
        int count = Integer.parseInt(arguments.getOrDefault("n", "100"));
        String prefix = arguments.getOrDefault("prefix", "acct_demo_");
        seedSynthetic(pool, count, prefix);
      }
    }
    System.out.println("Backfill complete.");
  }

  private static void seedSynthetic(JedisPool pool, int count, String prefix) {
    Random random = new Random(42);
    try (Jedis jedis = pool.getResource()) {
      for (int index = 0; index < count; index++) {
        String account = prefix + index;
        String key = "fs:acct:" + account;
        double amountZ = random.nextGaussian();
        int window15 = Math.max(0, (int) Math.round(random.nextGaussian() * 2 + 2));
        int first = random.nextDouble() < 0.1 ? 1 : 0;
        String mcc = random.nextDouble() < 0.05 ? "6540" : "5999";
        jedis.hset(
            key,
            Map.of(
                "amountZ", Double.toString(amountZ),
                "window15mCount", Integer.toString(window15),
                "firstTimeMerchant", Integer.toString(first),
                "mcc", mcc));
      }
    }
  }

  private static void seedFromCsv(JedisPool pool, Path csv) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(csv); Jedis jedis = pool.getResource()) {
      String line;
      boolean header = true;
      while ((line = reader.readLine()) != null) {
        if (header) {
          header = false;
          continue;
        }
        String[] columns = line.split(",", -1);
        String account = columns[0].trim();
        String key = "fs:acct:" + account;
        jedis.hset(
            key,
            Map.of(
                "amountZ", columns[1].trim(),
                "window15mCount", columns[2].trim(),
                "firstTimeMerchant", columns[3].trim(),
                "mcc", columns[4].trim()));
      }
    }
  }

  private static Map<String, String> parseArgs(String[] args) {
    Map<String, String> parsed = new HashMap<>();
    for (String arg : args) {
      if (arg.startsWith("--")) {
        int equals = arg.indexOf('=');
        if (equals > 2) {
          parsed.put(arg.substring(2, equals), arg.substring(equals + 1));
        }
      }
    }
    return parsed;
  }
}
