package com.example.demo.user.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "users_seq", allocationSize = 50)
    private Long id;
    @NotBlank(message = "User must have a first name.")
    @Column(name = "first_name")
    private String firstName;
    @NotBlank(message = "User must have a last name.")
    private String lastName;
    @Email(message = "User must have a valid email")
    @NotBlank(message = "User must have an email")
    @Column(unique=true)
    private String email;
    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
    @Column(nullable = false)
    @NotBlank
    private String hashPassword;
    public User(){}
    public User(LocalDate birthDate, String email, String lastName, String firstName) {
        this.birthDate = birthDate;
        this.email = email;
        this.lastName = lastName;
        this.firstName = firstName;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getHashPassword() {
        return hashPassword;
    }

    public void setHashPassword(String hashPassword) {
        this.hashPassword = hashPassword;
    }

}
