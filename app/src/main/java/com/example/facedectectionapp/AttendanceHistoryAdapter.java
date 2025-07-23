package com.example.facedectectionapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AttendanceHistoryAdapter extends RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder> {

    private final List<AttendanceRecord> attendanceList;
    private final Context context;

    public AttendanceHistoryAdapter(Context context, List<AttendanceRecord> attendanceList) {
        this.context = context;
        this.attendanceList = attendanceList;
    }

    @NonNull
    @Override
    public AttendanceHistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceHistoryAdapter.ViewHolder holder, int position) {
        AttendanceRecord record = attendanceList.get(position);

        String name = record.getStudentName();
        String reg = record.getRegNumber();

        // Use studentId to determine if it's unknown
        if (record.getStudentId() == -1) {
            name = "Unknown";
            reg = "N/A";
        }

        holder.studentName.setText(name);
        holder.regNumber.setText(reg);
        holder.attendanceStatus.setText(record.isPresent() ? "Present" : "Absent");

        int color = record.isPresent()
                ? ContextCompat.getColor(context, android.R.color.holo_green_dark)
                : ContextCompat.getColor(context, android.R.color.holo_red_dark);
        holder.attendanceStatus.setTextColor(color);
    }



    @Override
    public int getItemCount() {
        return attendanceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView studentName, regNumber, attendanceStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            studentName = itemView.findViewById(R.id.studentName);
            regNumber = itemView.findViewById(R.id.regNumber);
            attendanceStatus = itemView.findViewById(R.id.attendanceStatus);
        }
    }
}
