package com.example.facedectectionapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Button btnAddStudent, btnMarkAttendance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAddStudent = findViewById(R.id.btnAddStudent);
        btnMarkAttendance = findViewById(R.id.btnMarkAttendance);

        btnAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddStudentActivity.class);
            startActivity(intent);
        });


        btnMarkAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MarkAttendanceActivity.class);
            startActivity(intent);
        });

        Button btnHistory = findViewById(R.id.btnViewHistory);
        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AttendanceHistoryActivity.class));
        });

    }
}
