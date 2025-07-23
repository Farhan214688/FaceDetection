package com.example.facedectectionapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AttendanceHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AttendanceHistoryAdapter adapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_history);

        recyclerView = findViewById(R.id.recyclerAttendance);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);
        List<AttendanceRecord> logs = dbHelper.getAttendanceHistory();

        adapter = new AttendanceHistoryAdapter(AttendanceHistoryActivity.this, logs);
        recyclerView.setAdapter(adapter);
    }
}
