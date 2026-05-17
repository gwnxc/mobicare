package com.example.mobicare;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Sets the top status bar color perfectly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#155A91"));
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        // Notice we deleted all the EdgeToEdge and WindowInsets padding code!
        // Android will now handle the bottom navigation buttons natively.
    }
}