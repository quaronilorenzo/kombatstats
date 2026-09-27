package com.example.demo.usersport.repository;

import com.example.demo.sport.entity.SportType;
import com.example.demo.usersport.entity.UserSport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSportRepository extends JpaRepository<UserSport, Long> {

    List<UserSport> findByUserId(Long idUser);

    boolean existsByUserIdAndSportSportType(Long idUser, SportType sportType);
}
