package com.example.facedectectionapp;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceNetModel {

    private static final String TAG = "FaceNetModel";
    private static final int INPUT_IMAGE_SIZE = 112;
    private static final int EMBEDDING_SIZE = 192;
    private static final int NUM_CHANNELS = 3;

    private final Interpreter interpreter;

    public FaceNetModel(AssetManager assetManager, String modelFileName) throws IOException {
        interpreter = new Interpreter(loadModelFile(assetManager, modelFileName));
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    public float[] getEmbedding(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Input bitmap is null");
            return null;
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);
        ByteBuffer inputBuffer = convertBitmapToBuffer(resizedBitmap);

        float[][] output = new float[1][EMBEDDING_SIZE];
        try {
            interpreter.run(inputBuffer, output);
        } catch (Exception e) {
            Log.e(TAG, " Failed to run model inference: " + e.getMessage());
            return null;
        }

        return l2Normalize(output[0]);
    }

    private ByteBuffer convertBitmapToBuffer(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1 * INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE * NUM_CHANNELS * 4);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_IMAGE_SIZE, 0, 0, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE);

        for (int pixel : pixels) {
            float r = ((pixel >> 16) & 0xFF) / 128.0f - 1.0f;
            float g = ((pixel >> 8) & 0xFF) / 128.0f - 1.0f;
            float b = (pixel & 0xFF) / 128.0f - 1.0f;

            buffer.putFloat(r);
            buffer.putFloat(g);
            buffer.putFloat(b);
        }

        return buffer;
    }

    private float[] l2Normalize(float[] embedding) {
        float norm = 0f;
        for (float val : embedding) {
            norm += val * val;
        }
        norm = (float) Math.sqrt(norm);

        if (norm == 0f) {
            Log.w(TAG, "⚠ Embedding norm is zero. Returning raw embedding.");
            return embedding;
        }

        float[] normalized = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            normalized[i] = embedding[i] / norm;
        }

        return normalized;
    }

    public void close() {
        interpreter.close();
    }
}
