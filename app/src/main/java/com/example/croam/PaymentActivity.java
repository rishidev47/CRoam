package com.example.croam;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends Activity implements PaymentResultListener {
    private static final String TAG = PaymentActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment);

        /*
         To ensure faster loading of the Checkout form,
          call this method as early as possible in your checkout flow.
         */
        Checkout.preload(getApplicationContext());

        // Payment button created by you in XML layout
        Button button = findViewById(R.id.btn_pay);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPayment();
            }
        });

//        TextView privacyPolicy = (TextView) findViewById(R.id.txt_privacy_policy);

//        privacyPolicy.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent httpIntent = new Intent(Intent.ACTION_VIEW);
//                httpIntent.setData(Uri.parse("https://razorpay.com/sample-application/"));
//                startActivity(httpIntent);
//            }
//        });
    }

    public void startPayment() {
        /*
          You need to pass current activity in order to let Razorpay create CheckoutActivity
         */
        final Activity activity = this;

        final Checkout co = new Checkout();
        co.setKeyID("rzp_test_kf1lhLZ4vLHEn0");

        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                    this.getApplicationContext()); //Get the preferences
            String phone = prefs.getString("phone", null); //get a String
            String name = prefs.getString("name", null); //get a String
            String email = prefs.getString("email", null); //get a String

            JSONObject options = new JSONObject();
            options.put("name", name);
            options.put("description", "Premium Membership");
            //You can omit the image option to fetch the image from dashboard
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png");
            options.put("currency", "INR");
            options.put("amount", "2500");

            JSONObject preFill = new JSONObject();
            preFill.put("email", email);
            preFill.put("contact", phone);

            options.put("prefill", preFill);

            co.open(activity, options);
        } catch (Exception e) {
            Toast.makeText(activity, "Error in payment: " + e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
            e.printStackTrace();
        }
    }

    /**
     * The name of the function has to be
     * onPaymentSuccess
     * Wrap your code in try catch, as shown, to ensure that this method runs correctly
     */
    @SuppressWarnings("unused")
    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {

//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
//                getApplicationContext());
//        Call<ResponseBody> call = MyApi.Companion.invoke().setPremium(
//                Objects.requireNonNull(prefs.getString("phone", null)), output);
//        Objects.requireNonNull(call).enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call,
//                    Response<ResponseBody> response) {
//                try {
//                    Log.e("payment", response.body().string());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//
//            }
//        });
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            Calendar c = Calendar.getInstance();
            Date currentTime = c.getTime();
            c.setTime(currentTime);
            c.add(Calendar.DATE, 365);
            String output = sdf.format(c.getTime());
            Log.e("pay", output);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                    getApplicationContext());
            Call<ResponseBody> call = MyApi.Companion.invoke().setPremium(
                    Objects.requireNonNull(prefs.getString("phone", null)), output);
            Objects.requireNonNull(call).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call,
                        Response<ResponseBody> response) {
                    try {
                        Log.e("payment", response.body().string());
                        Intent intent = new Intent(PaymentActivity.this, MainActivity.class);
                        startActivity(intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {

                }
            });
            Toast.makeText(this, "Payment Successful: " + razorpayPaymentID,
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The name of the function has to be
     * onPaymentError
     * Wrap your code in try catch, as shown, to ensure that this method runs correctly
     */
    @SuppressWarnings("unused")
    @Override
    public void onPaymentError(int code, String response) {
        try {
            Toast.makeText(this, "Payment failed: " + code + " " + response,
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Exception in onPaymentError", e);
        }
    }
}