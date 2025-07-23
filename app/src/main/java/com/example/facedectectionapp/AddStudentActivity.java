package com.example.facedectectionapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class AddStudentActivity extends AppCompatActivity {

    EditText editName, editRegNumber;
    Button btnCaptureFace, btnSaveStudent;
    ImageView imageViewFace;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int CAMERA_PERMISSION_CODE = 100;

    List<float[]> embeddingsList = new ArrayList<>();
    Bitmap lastCapturedFaceBitmap = null;

    FaceNetModel faceNetModel;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        editName = findViewById(R.id.editName);
        editRegNumber = findViewById(R.id.editRegNumber);
        btnCaptureFace = findViewById(R.id.btnCaptureFace);
        btnSaveStudent = findViewById(R.id.btnSaveStudent);
        imageViewFace = findViewById(R.id.imageViewFace);

        db = new DatabaseHelper(this);

        try {
            faceNetModel = new FaceNetModel(getAssets(), "mobilefacenet.tflite");
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load model", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnCaptureFace.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                openCamera();
            }
        });

        btnSaveStudent.setOnClickListener(v -> saveStudent());
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            detectAndCropFace(bitmap);
        }
    }

    private void detectAndCropFace(Bitmap originalBitmap) {
        InputImage image = InputImage.fromBitmap(originalBitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        Face face = faces.get(0);
                        Rect box = face.getBoundingBox();

                        int x = Math.max(box.left, 0);
                        int y = Math.max(box.top, 0);
                        int width = Math.min(box.width(), originalBitmap.getWidth() - x);
                        int height = Math.min(box.height(), originalBitmap.getHeight() - y);

                        lastCapturedFaceBitmap = Bitmap.createBitmap(originalBitmap, x, y, width, height);
                        imageViewFace.setImageBitmap(lastCapturedFaceBitmap);

                        float[] embedding = faceNetModel.getEmbedding(lastCapturedFaceBitmap);
                        if (embedding != null && embedding.length == 192) {
                            embeddingsList.add(embedding);
                            Toast.makeText(this, " Face captured. Total samples: " + embeddingsList.size(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, " Embedding failed.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, " No face detected", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "⚠ Detection failed", Toast.LENGTH_SHORT).show());
    }

    private void saveStudent() {
        String name = editName.getText().toString().trim();
        String reg = editRegNumber.getText().toString().trim();

        if (name.isEmpty() || reg.isEmpty() || embeddingsList.isEmpty() || lastCapturedFaceBitmap == null) {
            Toast.makeText(this, " Fill all fields and capture at least one face", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save image to internal storage
        File file = new File(getFilesDir(), "face_" + reg + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            lastCapturedFaceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (Exception e) {
            Toast.makeText(this, " Failed to save image", Toast.LENGTH_SHORT).show();
            return;
        }

        Student student = new Student(name, reg, file.getAbsolutePath(), embeddingsList);
        boolean success = db.addStudent(student);

        if (success) {
            Toast.makeText(this, " Student saved with " + embeddingsList.size() + " face sample(s)", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, " Failed to save student", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (faceNetModel != null) faceNetModel.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, " Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
