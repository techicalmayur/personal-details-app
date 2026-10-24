package com.example.personaldetails.model;

import java.io.Serializable;
import java.time.LocalDate;

/** A saved record. Serializable because it lives in the HTTP session. */
public record PersonalDetails(
        String id,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String address,
        String city,
        String country
) implements Serializable {

    public static PersonalDetails of(String id, PersonalDetailsRequest r) {
        return new PersonalDetails(id, r.firstName().trim(), r.lastName().trim(), r.email().trim(),
                blankToNull(r.phone()), r.dateOfBirth(), blankToNull(r.address()),
                blankToNull(r.city()), blankToNull(r.country()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
