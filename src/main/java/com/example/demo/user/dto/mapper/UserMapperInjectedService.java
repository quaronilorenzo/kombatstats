package com.example.demo.user.dto.mapper;

import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class UserMapperInjectedService
{
    abstract UserRequest userToUserRequest(User user);
    abstract User userRequestToUser(UserRequest userRequest);
}
