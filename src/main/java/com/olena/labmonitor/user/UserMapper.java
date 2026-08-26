package com.olena.labmonitor.user;

import com.olena.labmonitor.membership.Membership;
import com.olena.labmonitor.user.dto.UserResponse;
import com.olena.labmonitor.user.dto.UserResponse.MembershipInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    //MapStruct automatically generates the implementations
    @Mapping(source = "memberships", target = "memberships", qualifiedByName = "toMembershipInfo")
    UserResponse toResponse(User user);

    List<UserResponse> toResponses(List<User> users);

    // Instructs MapStruct to convert a Membership field in UserResponce
    @Named("toMembershipInfo")
    default MembershipInfo membershipToInfo(Membership membership) {
        return new MembershipInfo(
                membership.getOrganization().getId(),
                membership.getOrganization().getName(),
                membership.getRole()
        );
    }
}
