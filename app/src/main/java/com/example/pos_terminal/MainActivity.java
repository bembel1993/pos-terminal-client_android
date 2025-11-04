package com.example.pos_terminal;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    String msg = "Android : ";
    private EditText etCardNumber, etAmount, etMerchantId;
    private Button btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(msg, "The onCreate() event");

        etCardNumber = findViewById(R.id.etCardNumber);
        etAmount = findViewById(R.id.etAmount);
        etMerchantId = findViewById(R.id.etMerchantId);
//        btnSend = findViewById(R.id.btnSend);
    }

            public void onClick(View v) throws IOException {
                String cardNumber = etCardNumber.getText().toString().trim();
                String amountStr = etAmount.getText().toString().trim();
                String merchantIdStr = etMerchantId.getText().toString().trim();

                if (cardNumber.isEmpty() || amountStr.isEmpty() || merchantIdStr.isEmpty()) {
                    Log.e("MainActivity", "Пожалуйста, заполните все поля");
                    return;
                }

                int amountCents = 0;
                int merchantId = 0;

                try {
                    amountCents = Integer.parseInt(amountStr);
                } catch (NumberFormatException e) {
                    System.out.println("Некорректное значение суммы");
                    amountCents = 0;
                }

                try {
                    merchantId = Integer.parseInt(merchantIdStr);
                } catch (NumberFormatException e) {
                    System.out.println("Некорректный ID магазина");
                    merchantId = 0;
                }


                byte[] transactionBytes = createTransaction(cardNumber, amountCents, merchantId);

                TransactionData data = decodeTransaction(transactionBytes);

                Intent intent = new Intent(this, SuccessActivity.class);
                intent.putExtra(TransactionData.class.getSimpleName(), data);

                sendDataToServer(cardNumber, amountCents, merchantId);
                startActivity(intent);

            }

    private void sendDataToServer(String cardNumber, int amountCents, int merchantId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://IPv4:12345/api/transaction");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json");

                    byte[] transactionBytes = createTransaction(cardNumber, amountCents, merchantId);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("transactionBytes", bytesToHex(transactionBytes));

                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(jsonParam.toString());
                    writer.flush();
                    writer.close();
                    os.close();

                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream is = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        String line;
                        StringBuilder response = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        Log.d("ServerResponse", response.toString());

                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Данные успешно отправлены!", Toast.LENGTH_SHORT).show());

                    } else {
                        Log.e("ServerError", "Response code: " + responseCode);

                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ошибка при отправке данных. Код: " + responseCode, Toast.LENGTH_SHORT).show());
                    }

                    conn.disconnect();

                } catch (Exception e) {
                    e.printStackTrace();

                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    System.out.println("Ошибка: " + e.getMessage());
                }
            }
        }).start();
    }

    public void getDataFromServer() throws IOException {
        URL url = new URL("http://10.192.112.148:12345/api/endpoint");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

    }


    // Метод для маскировки номера карты
    public static String maskCardNumber(String cardNumber) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cardNumber.length(); i++) {
            if (i < 4 || i >= cardNumber.length() - 4) {
                sb.append(cardNumber.charAt(i));
            } else {
                sb.append('*');
            }
        }
        return sb.toString();
    }

    // Метод для генерации уникального transactionId
    public static String generateTransactionId() {
        UUID uuid = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        return uuid.toString() + "_" + timestamp;
    }

    // Метод для получения байтов фиксированной длины
    public static byte[] fixedLengthBytes(String str, int length) {
        byte[] bytes = new byte[length];
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        int copyLength = Math.min(strBytes.length, length);
        System.arraycopy(strBytes, 0, bytes, 0, copyLength);
        // Остальные байты остаются нулями
        return bytes;
    }

    // Основной метод создания транзакции
    public static byte[] createTransaction(String cardNumber, int amountCents, int merchantId) {
        String maskedCard = maskCardNumber(cardNumber);
        String transactionId = generateTransactionId();

        final int CARD_LEN = 20;
        final int TRAN_ID_LEN = 50;

        byte[] cardBytes = fixedLengthBytes(maskedCard, CARD_LEN);
        byte[] transIdBytes = fixedLengthBytes(transactionId, TRAN_ID_LEN);

        ByteBuffer buffer = ByteBuffer.allocate(CARD_LEN + 4 + TRAN_ID_LEN + 4);

        buffer.put(cardBytes);
        buffer.putInt(amountCents);
        buffer.put(transIdBytes);
        buffer.putInt(merchantId);

        return buffer.array();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes){
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    public static TransactionData decodeTransaction(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        byte[] cardBytes = new byte[20];
        buffer.get(cardBytes);

        String card = new String(cardBytes, StandardCharsets.UTF_8).trim();
        int amount = buffer.getInt();
        byte[] transIdBytes = new byte[50];
        buffer.get(transIdBytes);
        String transId = new String(transIdBytes, StandardCharsets.UTF_8).trim();
        int merchantId = buffer.getInt();

        System.out.println("Card PAN: " + card);
        System.out.println("Amount: " + amount);
        System.out.println("Transaction ID: " + transId);
        System.out.println("Merchant ID: " + merchantId);

        TransactionData trData = new TransactionData(card, amount, transId, merchantId);

        return trData;
    }
}