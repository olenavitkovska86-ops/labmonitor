package com.olena.labmonitor.user;

import com.olena.labmonitor.user.dto.UserResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    //MapStruct automatically generates the implementations
    UserResponse toResponse(User user);
    List<UserResponse> toResponses(List<User> users);
}
