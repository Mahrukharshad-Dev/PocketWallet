package com.example.currencyconverter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class CurrencyAdapter extends ArrayAdapter<Currency> {

    public CurrencyAdapter(@NonNull Context context, @NonNull List<Currency> currencies) {
        super(context, android.R.layout.simple_spinner_item, currencies);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView view = (TextView) super.getView(position, convertView, parent);
        Currency currency = getItem(position);
        if (currency != null) {
            view.setText(currency.getDisplayString());
            view.setTextColor(getContext().getResources().getColor(R.color.text_primary, null));
            view.setTextSize(14f);
        }
        return view;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
        Currency currency = getItem(position);
        if (currency != null) {
            view.setText(currency.getDropdownString());
            view.setTextColor(getContext().getResources().getColor(R.color.text_primary, null));
            view.setTextSize(14f);
            int padding = (int) (12 * getContext().getResources().getDisplayMetrics().density);
            view.setPadding(padding, padding, padding, padding);
        }
        return view;
    }
}
