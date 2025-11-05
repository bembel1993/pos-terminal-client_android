package com.example.pos_terminal;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setTextSize(20);
        textView.setPadding(16, 16, 16, 16);

        Bundle arguments = getIntent().getExtras();

        TransactionData datatr;
        if(arguments!=null){
            datatr = (TransactionData) arguments.getSerializable(TransactionData.class.getSimpleName());

            textView.setText("Номер карты: " + datatr.getCardNumber() + "\nвнесенная сумма: " + datatr.getAmount() +
                    "\nID магазина: " + datatr.getMerchantId());
        }
        setContentView(textView);
    }

}
