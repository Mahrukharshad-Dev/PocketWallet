package com.example.currencyconverter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText etFromAmount, etToAmount;
    private Spinner spinnerFrom, spinnerTo;
    private com.google.android.material.button.MaterialButton btnConvert;
    private TextView tvExchangeRate, tvValidationError, tvHistoryContent;
    private ProgressBar progressBar;

    private final Map<String, Double> exchangeRates = new HashMap<>();
    private final List<Currency> currencyList = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SharedPreferences prefs;
    private static final String PREF_NAME = "MoneyMatePrefs";
    private static final String KEY_RATES = "cached_rates";
    private static final String KEY_HISTORY = "conversion_history";

    private final List<String> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Initialize UI Elements
        etFromAmount = findViewById(R.id.etFromAmount);
        etToAmount = findViewById(R.id.etToAmount);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        btnConvert = findViewById(R.id.btnConvert);
        tvExchangeRate = findViewById(R.id.tvExchangeRate);
        tvValidationError = findViewById(R.id.tvValidationError);
        tvHistoryContent = findViewById(R.id.tvHistoryContent);
        progressBar = findViewById(R.id.progressBar);

        initializeCurrencies();
        initializeDefaultRates();
        loadCachedRates();
        loadHistory();
        updateHistoryUI();

        CurrencyAdapter adapter = new CurrencyAdapter(this, currencyList);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        // Set default selection: GBP to PKR (matching Figure 1)
        for (int i = 0; i < currencyList.size(); i++) {
            if (currencyList.get(i).getCode().equals("GBP")) {
                spinnerFrom.setSelection(i);
            }
            if (currencyList.get(i).getCode().equals("PKR")) {
                spinnerTo.setSelection(i);
            }
        }

        // Convert button click listener
        btnConvert.setOnClickListener(v -> performConversion());

        // Fetch live rates
        fetchLiveExchangeRates();
    }

    private void initializeCurrencies() {
        currencyList.add(new Currency("USD", "US Dollar", "$", "🇺🇸"));
        currencyList.add(new Currency("EUR", "Euro", "€", "🇪🇺"));
        currencyList.add(new Currency("GBP", "British Pound", "£", "🇬🇧"));
        currencyList.add(new Currency("JPY", "Japanese Yen", "¥", "🇯🇵"));
        currencyList.add(new Currency("CAD", "Canadian Dollar", "CA$", "🇨🇦"));
        currencyList.add(new Currency("AUD", "Australian Dollar", "AU$", "🇦🇺"));
        currencyList.add(new Currency("CHF", "Swiss Franc", "CHF", "🇨🇭"));
        currencyList.add(new Currency("CNY", "Chinese Yuan", "¥", "🇨🇳"));
        currencyList.add(new Currency("INR", "Indian Rupee", "₹", "🇮🇳"));
        currencyList.add(new Currency("PKR", "Pakistani Rupee", "Rs", "🇵🇰"));
        currencyList.add(new Currency("AED", "UAE Dirham", "AED", "🇦🇪"));
        currencyList.add(new Currency("SAR", "Saudi Riyal", "SAR", "🇸🇦"));
    }

    private void initializeDefaultRates() {
        exchangeRates.put("USD", 1.0);
        exchangeRates.put("EUR", 0.92);
        exchangeRates.put("GBP", 0.79);
        exchangeRates.put("JPY", 155.0);
        exchangeRates.put("CAD", 1.37);
        exchangeRates.put("AUD", 1.50);
        exchangeRates.put("CHF", 0.90);
        exchangeRates.put("CNY", 7.25);
        exchangeRates.put("INR", 83.30);
        exchangeRates.put("PKR", 278.50);
        exchangeRates.put("AED", 3.67);
        exchangeRates.put("SAR", 3.75);
    }

    private void fetchLiveExchangeRates() {
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            try {
                URL url = new URL("https://api.frankfurter.app/latest?from=USD");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(sb.toString());
                    JSONObject ratesObj = jsonObject.getJSONObject("rates");

                    Map<String, Double> fetchedRates = new HashMap<>();
                    fetchedRates.put("USD", 1.0);
                    Iterator<String> keys = ratesObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        fetchedRates.put(key, ratesObj.getDouble(key));
                    }

                    // Save to cache
                    prefs.edit().putString(KEY_RATES, sb.toString()).apply();

                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        exchangeRates.clear();
                        exchangeRates.putAll(fetchedRates);
                    });
                } else {
                    throw new Exception("Server error");
                }
            } catch (Exception e) {
                mainHandler.post(() -> progressBar.setVisibility(View.GONE));
            }
        });
    }

    private void loadCachedRates() {
        String cachedJson = prefs.getString(KEY_RATES, null);
        if (cachedJson != null) {
            try {
                JSONObject jsonObject = new JSONObject(cachedJson);
                JSONObject ratesObj = jsonObject.getJSONObject("rates");
                exchangeRates.put("USD", 1.0);
                Iterator<String> keys = ratesObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    exchangeRates.put(key, ratesObj.getDouble(key));
                }
            } catch (JSONException e) {
                // Keep default rates
            }
        }
    }

    private void performConversion() {
        String inputText = etFromAmount.getText().toString().trim();

        if (TextUtils.isEmpty(inputText)) {
            tvValidationError.setVisibility(View.VISIBLE);
            tvValidationError.setText(getString(R.string.validation_error));
            etToAmount.setText("");
            return;
        }

        double inputAmount;
        try {
            inputAmount = Double.parseDouble(inputText);
        } catch (NumberFormatException e) {
            tvValidationError.setVisibility(View.VISIBLE);
            tvValidationError.setText(getString(R.string.validation_error));
            etToAmount.setText("");
            return;
        }

        if (inputAmount < 0) {
            tvValidationError.setVisibility(View.VISIBLE);
            tvValidationError.setText("Amount cannot be negative");
            etToAmount.setText("");
            return;
        }

        tvValidationError.setVisibility(View.GONE);

        Currency fromCurr = (Currency) spinnerFrom.getSelectedItem();
        Currency toCurr = (Currency) spinnerTo.getSelectedItem();

        String fromCode = fromCurr.getCode();
        String toCode = toCurr.getCode();

        Double rateFrom = exchangeRates.get(fromCode);
        Double rateTo = exchangeRates.get(toCode);

        if (rateFrom != null && rateTo != null) {
            double amountInUSD = inputAmount / rateFrom;
            double convertedAmount = amountInUSD * rateTo;
            double singleUnitRate = (1.0 / rateFrom) * rateTo;

            String formattedResult = String.format(Locale.getDefault(), "%s %,.2f", toCurr.getSymbol(), convertedAmount);
            etToAmount.setText(formattedResult);

            tvExchangeRate.setText(String.format(Locale.getDefault(), "Exchange rate: 1 %s = %.2f %s", fromCode, singleUnitRate, toCode));

            // Add to history matching Figure 3/6 format: £ 120.00 -> Rs 42,532.80
            String historyItem = String.format(Locale.getDefault(), "%s %,.2f -> %s %,.2f", 
                    fromCurr.getSymbol(), inputAmount, toCurr.getSymbol(), convertedAmount);
            addHistoryItem(historyItem);
        } else {
            Toast.makeText(this, "Conversion rate unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void addHistoryItem(String item) {
        if (!historyList.contains(item)) {
            historyList.add(0, item);
            if (historyList.size() > 5) {
                historyList.remove(historyList.size() - 1);
            }
            saveHistory();
            updateHistoryUI();
        }
    }

    private void saveHistory() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < historyList.size(); i++) {
            sb.append(historyList.get(i));
            if (i < historyList.size() - 1) {
                sb.append(";;");
            }
        }
        prefs.edit().putString(KEY_HISTORY, sb.toString()).apply();
    }

    private void loadHistory() {
        String historyStr = prefs.getString(KEY_HISTORY, "");
        if (!TextUtils.isEmpty(historyStr)) {
            String[] items = historyStr.split(";;");
            historyList.clear();
            for (String item : items) {
                if (!TextUtils.isEmpty(item)) {
                    historyList.add(item);
                }
            }
        }
    }

    private void updateHistoryUI() {
        if (historyList.isEmpty()) {
            tvHistoryContent.setText(getString(R.string.no_recent_conversions));
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < historyList.size(); i++) {
                sb.append(historyList.get(i));
                if (i < historyList.size() - 1) {
                    sb.append("\n");
                }
            }
            tvHistoryContent.setText(sb.toString());
        }
    }
}
