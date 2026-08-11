package com.olena.labmonitor.user;

import com.olena.labmonitor.user.dto.UserResponce;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    //MapStruct automatically generates the implementations
    UserResponce toResponse(User user);
    List<UserResponce> toResponses(List<User> users);

}
