package com.example.personaldetails.service;

public class StoreFullException extends RuntimeException {
    public StoreFullException(int max) {
        super("This session already holds " + max + " people. Delete one to add another.");
    }
}
