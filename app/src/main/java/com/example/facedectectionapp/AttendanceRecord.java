package com.example.facedectectionapp;

public class AttendanceRecord {
    private int studentId;
    private String studentName;
    private String regNumber;
    private boolean present;

    public AttendanceRecord(int studentId, String studentName, String regNumber, boolean present) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.regNumber = regNumber;
        this.present = present;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getRegNumber() {
        return regNumber;
    }

    public boolean isPresent() {
        return present;
    }
}
