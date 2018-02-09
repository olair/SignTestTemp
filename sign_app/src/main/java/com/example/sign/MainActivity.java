package com.example.sign;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TestView testView = findViewById(R.id.test_view);

        DisplayView displayView = findViewById(R.id.display_view);

    }


}
