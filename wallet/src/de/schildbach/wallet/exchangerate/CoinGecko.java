/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.exchangerate;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import de.schildbach.wallet.data.ExchangeRate;
import okhttp3.HttpUrl;
import okio.BufferedSource;
import org.bitcoinj.utils.Fiat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Andreas Schildbach
 */
public final class CoinGecko {
    private static final HttpUrl URL = HttpUrl.parse("https://api.coingecko.com/api/v3/exchange_rates");
    private static final String SOURCE = "CoinGecko.com";

    private static final Logger log = LoggerFactory.getLogger(CoinGecko.class);

    private final Moshi moshi;

    public CoinGecko(final Moshi moshi) {
        this.moshi = moshi;
    }

    public HttpUrl url() {
        return URL;
    }

    public Map<String, ExchangeRate> parse(final BufferedSource jsonSource) throws IOException {
        final JsonAdapter<Response> jsonAdapter = moshi.adapter(Response.class);
        final Response jsonResponse = jsonAdapter.fromJson(jsonSource);
        final Map<String, ExchangeRate> result = new TreeMap<>();
        for (Map.Entry<String, ExchangeRateJson> entry : jsonResponse.rates.entrySet()) {
            final String symbol = entry.getKey().toUpperCase(Locale.US);
            final ExchangeRateJson exchangeRate = entry.getValue();
            if (exchangeRate.type == Type.fiat) {
                try {
                    final Fiat rate = Fiat.parseFiatInexact(symbol, exchangeRate.value);
                    if (rate.signum() > 0)
                        result.put(symbol, new ExchangeRate(new org.bitcoinj.utils.ExchangeRate(rate), SOURCE));
                } catch (final ArithmeticException x) {
                    log.warn("problem parsing {} exchange rate from {}: {}", symbol, URL, x.getMessage());
                }
            }
        }
        return result;
    }

    private enum Type { crypto, fiat, commodity }

    private static class Response {
        public Map<String, ExchangeRateJson> rates;
    }

    private static class ExchangeRateJson {
        public String name;
        public String unit;
        public String value;
        public Type type;
    }
}
