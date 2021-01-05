package com.example.citizensafety;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class Report extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
    }

    public void goToSubmit(View view){
        Toast.makeText(getApplicationContext(), "Submitted Successfully", Toast.LENGTH_LONG).show();
    }
}
