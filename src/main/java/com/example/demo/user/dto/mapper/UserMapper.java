package com.example.demo.user.dto.mapper;

import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper
{
    UserRequest userToUserRequest(User user);
    User userRequestToUser(UserRequest userRequest);

    UserResponse userToUserResponse(User user);
    
    User userResponseToUser(UserResponse userResponse);
}
