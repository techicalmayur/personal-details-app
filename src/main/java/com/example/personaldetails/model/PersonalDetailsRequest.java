package com.example.personaldetails.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Payload accepted by POST and PUT. The server assigns the id. */
public record PersonalDetailsRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "Use 50 characters or fewer")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Use 50 characters or fewer")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 120, message = "Use 120 characters or fewer")
        String email,

        @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,20}$",
                 message = "Use 7-20 digits; spaces, +, ( ) and - are allowed")
        String phone,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @Size(max = 200, message = "Use 200 characters or fewer")
        String address,

        @Size(max = 60, message = "Use 60 characters or fewer")
        String city,

        @Size(max = 60, message = "Use 60 characters or fewer")
        String country
) {
}
