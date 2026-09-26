package com.example.demo.sport.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "sport")
public class Sport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sport")
    private Long idSport;

    @NotNull(message = "Sport must have a type")
    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false, unique = true)
    private SportType sportType;

    public Sport() {}

    public Sport(SportType sportType) {
        this.sportType = sportType;
    }

    public Long getIdSport() {
        return idSport;
    }

    public void setIdSport(Long idSport) {
        this.idSport = idSport;
    }

    public SportType getSportType() {
        return sportType;
    }

    public void setSportType(SportType sportType) {
        this.sportType = sportType;
    }
}
