package com.example.facedectectionapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a student with multiple face embeddings for better recognition.
 */
public class Student {

    private int id = -1;
    private String name;
    private String regNumber;
    private String imagePath;
    private List<float[]> embeddings = new ArrayList<>();

    /** Constructor without ID (for new students) */
    public Student(String name, String regNumber, String imagePath, List<float[]> embeddings) {
        this.name = name;
        this.regNumber = regNumber;
        this.imagePath = imagePath;
        this.embeddings = embeddings;
    }

    /** Constructor with ID (from DB) */
    public Student(int id, String name, String regNumber, String imagePath, List<float[]> embeddings) {
        this.id = id;
        this.name = name;
        this.regNumber = regNumber;
        this.imagePath = imagePath;
        this.embeddings = embeddings;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegNumber() {
        return regNumber;
    }

    public void setRegNumber(String regNumber) {
        this.regNumber = regNumber;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public List<float[]> getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(List<float[]> embeddings) {
        this.embeddings = embeddings;
    }

    public void addEmbedding(float[] embedding) {
        this.embeddings.add(embedding);
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", regNumber='" + regNumber + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", embeddingsCount=" + (embeddings != null ? embeddings.size() : 0) +
                '}';
    }
}
