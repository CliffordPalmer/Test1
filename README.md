# Coinbase Trading Bot Example

This project demonstrates a **minimal** Coinbase API trading bot in Java.

## Usage

1. Obtain API credentials from your Coinbase Exchange account.
2. Export the following environment variables:

```bash
export COINBASE_KEY=your_api_key
export COINBASE_SECRET=your_api_secret
export COINBASE_PASSPHRASE=your_api_passphrase
```

3. Compile and run the example:

```bash
javac src/CoinbaseTradingBot.java
java -cp src CoinbaseTradingBot
```

The `main` method now demonstrates a more complex strategy using indicators such as EMA, RSI, MACD, Bollinger Bands and VWAP. A `MAX_BUDGET` environment variable can be used to cap how much USD is spent in total (defaults to `$0.5`).

**DISCLAIMER:** This code is for educational purposes only. Trading cryptocurrency carries significant risk. You are solely responsible for any trades executed and must comply with applicable laws and the Coinbase API terms of service.
