package com.olena.labmonitor.user;

import com.olena.labmonitor.user.dto.UserResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    default UserResponse toResponse(User user) {
        return UserResponse.from(user);
    }

    default List<UserResponse> toResponses(List<User> users) {
        return users.stream().map(this::toResponse).toList();
    }
}
