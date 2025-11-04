package com.example.pos_terminal;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
                    "\nID транзакции: " + datatr.getTransId() + "\nID магазина: " + datatr.getMerchantId());
        }

//        if(arguments!=null){
//            String cardNumber = arguments.getString("etCardNumber");
//            String amountStr = arguments.getString("etAmount");
//            String merchantId = arguments.getString("etMerchantId");
//            String transactionBytes = arguments.getString("transactionBytes");
//            textView.setText("Номер карты: " + cardNumber + "\nвнесенная сумма: " + amountStr +
//                    "\nID магазина: " + merchantId +
//                    "\n Транзакция в виде бинарного сообщения: " + transactionBytes);
//        }

        setContentView(textView);
    }
}
