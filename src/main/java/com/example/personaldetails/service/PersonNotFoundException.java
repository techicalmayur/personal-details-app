package com.example.personaldetails.service;

public class PersonNotFoundException extends RuntimeException {
    public PersonNotFoundException(String id) {
        super("No saved person with id " + id + " in this session");
    }
}
