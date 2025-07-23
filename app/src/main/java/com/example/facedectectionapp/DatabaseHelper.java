package com.example.facedectectionapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";
    private static final String DATABASE_NAME = "FaceApp.db";
    private static final int DATABASE_VERSION = 5;

    private static final String TABLE_STUDENTS = "students";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_REG = "reg_number";
    private static final String COL_IMAGE_PATH = "image_path";
    private static final String COL_EMBEDDING = "embedding"; // Now stores all embeddings comma-separated by semicolon

    private static final String TABLE_ATTENDANCE = "attendance_log";
    private static final String COL_LOG_ID = "id";
    private static final String COL_STUDENT_ID = "student_id";
    private static final String COL_PRESENT = "present";

    private static final int EMBEDDING_SIZE = 192;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_STUDENTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT, " +
                COL_REG + " TEXT, " +
                COL_IMAGE_PATH + " TEXT, " +
                COL_EMBEDDING + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_ATTENDANCE + " (" +
                COL_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_STUDENT_ID + " INTEGER, " +
                COL_PRESENT + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        onCreate(db);
    }

    public boolean addStudent(Student student) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(COL_NAME, student.getName());
            values.put(COL_REG, student.getRegNumber());
            values.put(COL_IMAGE_PATH, student.getImagePath());
            values.put(COL_EMBEDDING, embeddingsToString(student.getEmbeddings()));
            long result = db.insert(TABLE_STUDENTS, null, values);
            return result != -1;
        }
    }

    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_STUDENTS, null)) {

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
                String reg = cursor.getString(cursor.getColumnIndexOrThrow(COL_REG));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH));
                List<float[]> embeddings = stringToEmbeddings(cursor.getString(cursor.getColumnIndexOrThrow(COL_EMBEDDING)));
                students.add(new Student(id, name, reg, path, embeddings));
            }
        }
        return students;
    }

    public void markAttendance(int studentId, boolean present) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(COL_STUDENT_ID, studentId);
            values.put(COL_PRESENT, present ? 1 : 0);
            db.insert(TABLE_ATTENDANCE, null, values);
        }
    }

    public void markUnknownAttendance(boolean present) {
        markAttendance(1, present);
    }

    public List<AttendanceRecord> getAttendanceHistory() {
        List<AttendanceRecord> history = new ArrayList<>();

        String query = "SELECT a." + COL_STUDENT_ID + ", a." + COL_PRESENT + ", " +
                "s." + COL_NAME + ", s." + COL_REG +
                " FROM " + TABLE_ATTENDANCE + " a " +
                "LEFT JOIN " + TABLE_STUDENTS + " s ON a." + COL_STUDENT_ID + " = s." + COL_ID;

        try (SQLiteDatabase db = this.getReadableDatabase();
             Cursor cursor = db.rawQuery(query, null)) {

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_STUDENT_ID));
                boolean present = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRESENT)) == 1;
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
                String reg = cursor.getString(cursor.getColumnIndexOrThrow(COL_REG));

                if (name == null) name = "Unknown";
                if (reg == null) reg = "-";

                history.add(new AttendanceRecord(id, name, reg, present));
            }
        }
        return history;
    }

    public Student findBestMatch(float[] detectedEmbedding, float threshold) {
        List<Student> students = getAllStudents();
        Student bestMatch = null;
        float bestDistance = Float.MAX_VALUE;

        for (Student student : students) {
            for (float[] knownEmbedding : student.getEmbeddings()) {
                if (knownEmbedding == null || knownEmbedding.length != EMBEDDING_SIZE) continue;

                float distance = calculateDistance(detectedEmbedding, knownEmbedding);
                if (distance < bestDistance && distance < threshold) {
                    bestDistance = distance;
                    bestMatch = student;
                }
            }
        }

        return bestMatch;
    }

    private float calculateDistance(float[] a, float[] b) {
        float sum = 0f;
        for (int i = 0; i < a.length; i++) {
            float diff = a[i] - b[i];
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum);
    }

    private String embeddingsToString(List<float[]> embeddings) {
        if (embeddings == null || embeddings.isEmpty()) return "";
        List<String> strings = new ArrayList<>();
        for (float[] emb : embeddings) {
            strings.add(embeddingToString(emb));
        }
        return TextUtils.join(";", strings);
    }

    private List<float[]> stringToEmbeddings(String data) {
        List<float[]> list = new ArrayList<>();
        if (data == null || data.isEmpty()) return list;
        String[] sets = data.split(";");
        for (String set : sets) {
            list.add(stringToEmbedding(set));
        }
        return list;
    }

    private String embeddingToString(float[] embedding) {
        if (embedding == null || embedding.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        return sb.toString();
    }

    private float[] stringToEmbedding(String str) {
        float[] embedding = new float[EMBEDDING_SIZE];
        if (str == null || str.isEmpty()) return embedding;

        String[] parts = str.split(",");
        for (int i = 0; i < EMBEDDING_SIZE && i < parts.length; i++) {
            try {
                embedding[i] = Float.parseFloat(parts[i]);
            } catch (NumberFormatException e) {
                embedding[i] = 0f;
            }
        }
        return embedding;
    }
}
