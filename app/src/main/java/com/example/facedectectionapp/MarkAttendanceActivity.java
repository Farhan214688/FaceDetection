package com.example.facedectectionapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MarkAttendanceActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final float MATCH_THRESHOLD = 1.0f;
    private static final int EMBEDDING_SIZE = 192;

    private Button btnDetect;
    private TextView textResult;
    private DatabaseHelper db;
    private FaceNetModel faceNetModel;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        btnDetect = findViewById(R.id.btnStartScan);
        textResult = findViewById(R.id.txtStatus);
        db = new DatabaseHelper(this);

        try {
            faceNetModel = new FaceNetModel(getAssets(), "mobilefacenet.tflite");
        } catch (Exception e) {
            Toast.makeText(this, " Model load failed!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                        detectMultipleFaces(photo);
                    }
                });

        btnDetect.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                openCamera();
            }
        });
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void detectMultipleFaces(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .enableTracking()
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        textResult.setText(" No faces detected.");
                        return;
                    }

                    textResult.setText(" Detected " + faces.size() + " face(s):\n");

                    Set<Integer> alreadyMarked = new HashSet<>();

                    for (Face face : faces) {
                        try {
                            Rect box = face.getBoundingBox();

                            int x = Math.max(box.left, 0);
                            int y = Math.max(box.top, 0);
                            int width = Math.min(box.width(), bitmap.getWidth() - x);
                            int height = Math.min(box.height(), bitmap.getHeight() - y);

                            Bitmap faceBitmap = Bitmap.createBitmap(bitmap, x, y, width, height);
                            Bitmap scaledFace = Bitmap.createScaledBitmap(faceBitmap, 112, 112, true);
                            float[] embedding = faceNetModel.getEmbedding(scaledFace);

                            if (embedding != null && embedding.length == EMBEDDING_SIZE) {
                                Student matchedStudent = db.findBestMatch(embedding, MATCH_THRESHOLD);

                                if (matchedStudent != null) {
                                    if (!alreadyMarked.contains(matchedStudent.getId())) {
                                        db.markAttendance(matchedStudent.getId(), true);
                                        alreadyMarked.add(matchedStudent.getId());
                                        textResult.append(" Present: " + matchedStudent.getName() + "\n");
                                    } else {
                                        textResult.append(" Already marked: " + matchedStudent.getName() + "\n");
                                    }
                                } else {
                                    db.markUnknownAttendance(false);
                                    textResult.append(" Unrecognized face – Marked Absent\n");
                                }
                            } else {
                                textResult.append(" Invalid embedding (null or incorrect size).\n");
                            }
                        } catch (Exception e) {
                            textResult.append("⚠ Face error: " + e.getMessage() + "\n");
                            Log.e("FACE_PROCESSING", "Error processing face: ", e);
                        }
                    }
                })
                .addOnFailureListener(e -> textResult.setText("⚠ Detection failed: " + e.getMessage()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (faceNetModel != null) {
            faceNetModel.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, " Camera permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
