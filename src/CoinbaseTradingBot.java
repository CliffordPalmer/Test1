import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

/**
 * Simple Coinbase trading bot example.
 *
 * DISCLAIMER: This example is for educational purposes only. Trading
 * cryptocurrencies involves significant risk. Use at your own risk
 * and ensure you comply with all regulations and Coinbase's API terms.
 */
public class CoinbaseTradingBot {

    private final String apiKey;
    private final String apiSecret;
    private final String passphrase;
    private final HttpClient client = HttpClient.newHttpClient();
    private static final String API_BASE = "https://api.exchange.coinbase.com";
    private double maxBudget = 0.5; // default maximum USD exposure
    private double spent = 0.0;

    public CoinbaseTradingBot(String apiKey, String apiSecret, String passphrase) {
        this(apiKey, apiSecret, passphrase, 0.5);
    }

    public CoinbaseTradingBot(String apiKey, String apiSecret, String passphrase, double maxBudget) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.passphrase = passphrase;
        this.maxBudget = maxBudget;
    }

    /**
     * Build a signed request for Coinbase Exchange API.
     */
    private HttpRequest buildRequest(String method, String path, String body) throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String prehash = timestamp + method.toUpperCase() + path + (body == null ? "" : body);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(Base64.getDecoder().decode(apiSecret), "HmacSHA256"));
        String signature = Base64.getEncoder().encodeToString(mac.doFinal(prehash.getBytes(StandardCharsets.UTF_8)));

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + path))
                .header("CB-ACCESS-KEY", apiKey)
                .header("CB-ACCESS-SIGN", signature)
                .header("CB-ACCESS-TIMESTAMP", timestamp)
                .header("CB-ACCESS-PASSPHRASE", passphrase)
                .header("Content-Type", "application/json");

        if ("POST".equalsIgnoreCase(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else {
            builder.GET();
        }
        return builder.build();
    }

    public String getAccounts() throws Exception {
        HttpRequest req = buildRequest("GET", "/accounts", null);
        return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static class Candle {
        long time;
        double low;
        double high;
        double open;
        double close;
        double volume;
    }

    private Candle[] fetchCandles(String productId, int granularity, int limit) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/products/" + productId + "/candles?granularity=" + granularity))
                .GET()
                .build();
        String body = client.send(req, HttpResponse.BodyHandlers.ofString()).body();
        if (body.startsWith("[")) {
            String[] rows = body.substring(1, body.length() - 1).split("\\],\\[");
            int count = Math.min(limit, rows.length);
            Candle[] candles = new Candle[count];
            for (int i = 0; i < count; i++) {
                String row = rows[i].replace("[", "").replace("]", "");
                String[] parts = row.split(",");
                if (parts.length >= 6) {
                    Candle c = new Candle();
                    c.time = Long.parseLong(parts[0]);
                    c.low = Double.parseDouble(parts[1]);
                    c.high = Double.parseDouble(parts[2]);
                    c.open = Double.parseDouble(parts[3]);
                    c.close = Double.parseDouble(parts[4]);
                    c.volume = Double.parseDouble(parts[5]);
                    candles[i] = c;
                }
            }
            return candles;
        }
        return new Candle[0];
    }

    public String placeMarketOrder(String productId, String side, String size) throws Exception {
        String body = String.format("{\"type\":\"market\",\"product_id\":\"%s\",\"side\":\"%s\",\"size\":\"%s\"}",
                productId, side, size);
        HttpRequest req = buildRequest("POST", "/orders", body);
        return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    /**
     * Example indicator: buys if price is below target.
     */
    private boolean shouldBuy(double price, double target) {
        return price < target;
    }

    public void runComplexStrategy(String productId, int granularity, int history, String size) throws Exception {
        Candle[] candles = fetchCandles(productId, granularity, history);
        if (candles.length == 0) {
            System.out.println("No candle data.");
            return;
        }
        double[] closes = new double[candles.length];
        double[] volumes = new double[candles.length];
        for (int i = 0; i < candles.length; i++) {
            closes[i] = candles[i].close;
            volumes[i] = candles[i].volume;
        }

        double emaFast = Indicators.ema(closes, 12);
        double emaSlow = Indicators.ema(closes, 26);
        double rsi = Indicators.rsi(closes, 14);
        double[] macd = Indicators.macd(closes, 12, 26, 9);
        double[] bb = Indicators.bollinger(closes, 20);
        double vwap = Indicators.vwap(closes, volumes);

        double lastPrice = closes[closes.length - 1];

        boolean buySignal = emaFast > emaSlow && rsi < 30 && macd[0] > macd[1] && lastPrice > bb[1] && lastPrice > vwap;

        if (buySignal) {
            double cost = lastPrice * Double.parseDouble(size);
            if (spent + cost > maxBudget) {
                System.out.println("Budget exceeded, not buying.");
                return;
            }
            String response = placeMarketOrder(productId, "buy", size);
            spent += cost;
            System.out.println("Order response: " + response);
        } else {
            System.out.println("No buy signal. Last price: " + lastPrice + " RSI:" + rsi);
        }
    }

    public void runExample(String productId, double targetPrice, String size) throws Exception {
        double currentPrice = fetchCurrentPrice(productId);
        if (shouldBuy(currentPrice, targetPrice)) {
            String response = placeMarketOrder(productId, "buy", size);
            System.out.println("Order response: " + response);
        } else {
            System.out.println("No action. Current price: " + currentPrice);
        }
    }

    private double fetchCurrentPrice(String productId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/products/" + productId + "/ticker"))
                .GET()
                .build();
        String body = client.send(req, HttpResponse.BodyHandlers.ofString()).body();
        String priceField = "\"price\":";
        int idx = body.indexOf(priceField);
        if (idx >= 0) {
            int start = idx + priceField.length();
            int end = body.indexOf(',', start);
            if (end == -1) end = body.indexOf('}', start);
            return Double.parseDouble(body.substring(start, end).replaceAll("\"", ""));
        }
        throw new IllegalStateException("Price not found: " + body);
    }

    public static void main(String[] args) throws Exception {
        String key = System.getenv("COINBASE_KEY");
        String secret = System.getenv("COINBASE_SECRET");
        String pass = System.getenv("COINBASE_PASSPHRASE");
        if (key == null || secret == null || pass == null) {
            System.err.println("Missing API credentials. Set COINBASE_KEY, COINBASE_SECRET, and COINBASE_PASSPHRASE environment variables.");
            return;
        }
        String budgetEnv = System.getenv("MAX_BUDGET");
        double budget = budgetEnv != null ? Double.parseDouble(budgetEnv) : 0.5;
        CoinbaseTradingBot bot = new CoinbaseTradingBot(key, secret, pass, budget);
        // run complex strategy on 5 minute candles using last 50 periods
        bot.runComplexStrategy("BTC-USD", 300, 50, "0.001");
    }
}
