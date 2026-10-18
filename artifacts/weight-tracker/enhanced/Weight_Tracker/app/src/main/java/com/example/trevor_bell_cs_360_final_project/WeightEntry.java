package com.example.trevor_bell_cs_360_final_project;

public class WeightEntry {

    // Unique identifier for the entry (used for edit/delete)
    private int id;

    // Weight value in pounds
    private double weight;

    // Optional notes provided by the user
    private String notes;

    // Constructor to initialize all fields
    public WeightEntry(int id, double weight, String notes) {
        this.id = id;
        this.weight = weight;
        this.notes = notes;
    }

    // Getter for entry ID
    public int getId() {
        return id;
    }

    // Getter for weight value
    public double getWeight() {
        return weight;
    }

    // Getter for notes
    public String getNotes() {
        return notes;
    }
}