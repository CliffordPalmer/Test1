public class Indicators {
    public static double ema(double[] prices, int period) {
        if (prices.length < period) return Double.NaN;
        double k = 2.0 / (period + 1);
        double ema = prices[0];
        for (int i = 1; i < prices.length; i++) {
            ema = prices[i] * k + ema * (1 - k);
        }
        return ema;
    }

    public static double sma(double[] prices, int period) {
        if (prices.length < period) return Double.NaN;
        double sum = 0;
        for (int i = prices.length - period; i < prices.length; i++) {
            sum += prices[i];
        }
        return sum / period;
    }

    public static double rsi(double[] prices, int period) {
        if (prices.length < period + 1) return Double.NaN;
        double gain = 0;
        double loss = 0;
        for (int i = prices.length - period; i < prices.length; i++) {
            double change = prices[i] - prices[i - 1];
            if (change > 0) gain += change; else loss -= change;
        }
        double rs = loss == 0 ? 100 : gain / loss;
        return 100 - (100 / (1 + rs));
    }

    public static double[] macd(double[] prices, int fast, int slow, int signal) {
        double[] macdLine = new double[prices.length];
        double[] emaFast = new double[prices.length];
        double[] emaSlow = new double[prices.length];
        double kFast = 2.0 / (fast + 1);
        double kSlow = 2.0 / (slow + 1);
        emaFast[0] = prices[0];
        emaSlow[0] = prices[0];
        for (int i = 1; i < prices.length; i++) {
            emaFast[i] = prices[i] * kFast + emaFast[i - 1] * (1 - kFast);
            emaSlow[i] = prices[i] * kSlow + emaSlow[i - 1] * (1 - kSlow);
            macdLine[i] = emaFast[i] - emaSlow[i];
        }
        double[] signalLine = new double[prices.length];
        double kSig = 2.0 / (signal + 1);
        signalLine[0] = macdLine[0];
        for (int i = 1; i < prices.length; i++) {
            signalLine[i] = macdLine[i] * kSig + signalLine[i - 1] * (1 - kSig);
        }
        return new double[] { macdLine[prices.length - 1], signalLine[prices.length - 1] };
    }

    public static double[] bollinger(double[] prices, int period) {
        double sma = sma(prices, period);
        if (Double.isNaN(sma)) return new double[] { Double.NaN, Double.NaN };
        double sumSq = 0;
        for (int i = prices.length - period; i < prices.length; i++) {
            double diff = prices[i] - sma;
            sumSq += diff * diff;
        }
        double std = Math.sqrt(sumSq / period);
        return new double[] { sma - 2 * std, sma + 2 * std };
    }

    public static double vwap(double[] closes, double[] volumes) {
        double numer = 0;
        double denom = 0;
        for (int i = 0; i < closes.length; i++) {
            numer += closes[i] * volumes[i];
            denom += volumes[i];
        }
        return denom == 0 ? Double.NaN : numer / denom;
    }
}
