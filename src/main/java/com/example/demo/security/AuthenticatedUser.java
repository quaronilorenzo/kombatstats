package com.example.demo.security;

import com.example.demo.user.entity.Role;

public record AuthenticatedUser(Long id, Role role) {}
