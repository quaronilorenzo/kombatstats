package com.example.demo.user.service;

import com.example.demo.user.entity.Sport;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    public User addUser(String firstName, String lastName, String email, Date birthDate, List<Sport> sport) {
        User user = new User(sport, birthDate, email, lastName, firstName);
        return userRepository.save(user);
    }
    public List<User> findAll(){
        return userRepository.findAll();
    }
    public Optional<User> findUserById(int id){
        return userRepository.findById(id);
    }
    public Optional<User> findUserByFirstname(String name){
        return userRepository.findByFirstName(name);
    }
}
