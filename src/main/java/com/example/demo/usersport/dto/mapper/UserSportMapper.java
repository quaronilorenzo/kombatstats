package com.example.demo.usersport.dto.mapper;

import com.example.demo.usersport.dto.UserSportResponse;
import com.example.demo.usersport.entity.UserSport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserSportMapper {

    @Mapping(source = "sport.sportType", target = "sportType")

    @Mapping(source = "competing", target = "isCompeting")
    UserSportResponse userSportToUserSportResponse(UserSport userSport);

    List<UserSportResponse> userSportsToUserSportResponses(List<UserSport> userSports);
}
