package com.example.demo.usersport.service;

import com.example.demo.sport.entity.Sport;
import com.example.demo.sport.entity.SportType;
import com.example.demo.sport.exceptions.SportNotFoundException;
import com.example.demo.sport.repository.SportRepository;
import com.example.demo.user.entity.User;
import com.example.demo.usersport.dto.UserSportRequest;
import com.example.demo.usersport.entity.UserSport;
import com.example.demo.usersport.exceptions.DuplicatedUserSportException;
import com.example.demo.usersport.repository.UserSportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserSportService {

    private final UserSportRepository userSportRepository;
    private final SportRepository sportRepository;

    public UserSportService(UserSportRepository userSportRepository, SportRepository sportRepository) {
        this.userSportRepository = userSportRepository;
        this.sportRepository = sportRepository;
    }
    @Transactional
    public List<UserSport> createForUser(User user, List<UserSportRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        Set<SportType> requestedTypes = new LinkedHashSet<>();
        for (UserSportRequest request : requests) {
            if (!requestedTypes.add(request.sportType())) {
                throw new DuplicatedUserSportException(request.sportType());
            }
        }

        Map<SportType, Sport> sportsByType = new EnumMap<>(SportType.class);
        sportRepository.findBySportTypeIn(requestedTypes)
                .forEach(sport -> sportsByType.put(sport.getSportType(), sport));

        Set<SportType> missing = new LinkedHashSet<>(requestedTypes);
        missing.removeAll(sportsByType.keySet());
        if (!missing.isEmpty()) {
            throw new SportNotFoundException(missing);
        }

        List<UserSport> toSave = requests.stream()
                .map(request -> new UserSport(
                        user,
                        sportsByType.get(request.sportType()),
                        request.yearsPracticed(),
                        request.isCompeting()))
                .toList();

        return userSportRepository.saveAll(toSave);
    }

    @Transactional(readOnly = true)
    public List<UserSport> findByUserId(Long idUser) {
        return userSportRepository.findByUserId(idUser);
    }
}
