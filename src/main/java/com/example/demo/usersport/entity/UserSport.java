package com.example.demo.usersport.entity;

import com.example.demo.sport.entity.Sport;
import com.example.demo.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Sport praticato da un utente: la tabella ponte della many-to-many fra users e sport.
 *
 * yearsPracticed e isCompeting stanno qui e non su {@link Sport} perche' dipendono
 * dalla coppia (utente, sport). Marco puo' fare BJJ da 5 anni e Luca da 2 puntando
 * entrambi alla stessa riga di anagrafica.
 *
 * Le due @ManyToOne sono LAZY di proposito: User viene serializzato direttamente dal
 * controller, e un fetch eager qui trascinerebbe in JSON l'intero grafo.
 */
@Entity
@Table(
        name = "user_sport",
        uniqueConstraints = @UniqueConstraint(
                name = "user_sport_user_sport_unique",
                columnNames = {"id_user", "id_sport"}
        )
)
public class UserSport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user_sport")
    private Long idUserSport;

    @NotNull(message = "User sport must reference a user")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @NotNull(message = "User sport must reference a sport")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sport", nullable = false)
    private Sport sport;

    @PositiveOrZero(message = "Years practiced must not be negative")
    @Column(name = "years_practiced", precision = 4, scale = 1)
    private BigDecimal yearsPracticed;

    @Column(name = "is_competing", nullable = false)
    private boolean isCompeting;

    public UserSport() {}

    public UserSport(User user, Sport sport, BigDecimal yearsPracticed, boolean isCompeting) {
        this.user = user;
        this.sport = sport;
        this.yearsPracticed = yearsPracticed;
        this.isCompeting = isCompeting;
    }

    public Long getIdUserSport() {
        return idUserSport;
    }

    public void setIdUserSport(Long idUserSport) {
        this.idUserSport = idUserSport;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
        this.sport = sport;
    }

    public BigDecimal getYearsPracticed() {
        return yearsPracticed;
    }

    public void setYearsPracticed(BigDecimal yearsPracticed) {
        this.yearsPracticed = yearsPracticed;
    }

    public boolean isCompeting() {
        return isCompeting;
    }

    public void setCompeting(boolean competing) {
        this.isCompeting = competing;
    }
}
