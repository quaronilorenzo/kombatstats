package com.example.demo.service;

import com.example.demo.entity.Sport;
import com.example.demo.entity.User;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    public User addUser(String firstName, String lastName, String email, Date birthDate, List<Sport> sport) {
        User user = new User(sport, birthDate, email, lastName, firstName, null);
        return userRepository.save(user);
    }
}
