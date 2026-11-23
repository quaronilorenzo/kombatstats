package com.example.demo.config;

import com.example.demo.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    @Autowired
    UserService userService;
    @Autowired
    AdminBootstrapProperties properties;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        userService.createInitialAdmin(properties);
    }
}
