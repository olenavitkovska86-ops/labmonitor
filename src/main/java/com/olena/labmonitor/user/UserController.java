package com.olena.labmonitor.user;

import com.olena.labmonitor.user.dto.CreateUserRequest;
import com.olena.labmonitor.user.dto.UpdateUserRequest;
import com.olena.labmonitor.user.dto.UserResponce;
import jakarta.validation.Valid;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService){this.userService = userService;}

    @GetMapping("/{id}")
    public UserResponce findById(@PathVariable Long profile){return userService.findById(profile);}

    @GetMapping("/{profile")
    public UserResponce getMyProfile(Authentication authentication){
        String email = authentication.getDeclaringClass().getName();
        return userService.findMe(email);
    }

    @PutMapping("/{id}")
    public UserResponce update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request){
        return userService.update(id, request);
    }



    // Super Admin ONLY




}
