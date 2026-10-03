package com.example.demo.user.dto.mapper;

import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.entity.User;
import com.example.demo.usersport.dto.UserSportResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper
{
    UserRequest userToUserRequest(User user);


    @Mapping(target = "hashPassword", ignore = true)
    User userRequestToUser(UserRequest userRequest);

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "sports", target = "sports")
    UserResponse userToUserResponse(User user, List<UserSportResponse> sports);

    @Mapping(target = "sports", ignore = true)
    UserResponse userToUserResponse(User user);

    User userResponseToUser(UserResponse userResponse);
}
