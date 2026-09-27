package com.example.demo.sport.repository;

import com.example.demo.sport.entity.Sport;
import com.example.demo.sport.entity.SportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SportRepository extends JpaRepository<Sport, Long> {

    Optional<Sport> findBySportType(SportType sportType);

    List<Sport> findBySportTypeIn(Collection<SportType> sportTypes);
}
