package com.example.demo.entity;
import jakarta.persistence.*;

import java.util.Date;
import java.util.List;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Date birthDate;
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<Sport> sport;
}

