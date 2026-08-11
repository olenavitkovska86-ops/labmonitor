package com.olena.labmonitor.user;

import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.user.dto.CreateUserRequest;
import com.olena.labmonitor.user.dto.UpdateUserRequest;
import com.olena.labmonitor.user.dto.UserResponce;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper){
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }


    @Transactional(readOnly = true)
    public UserResponce findById(Long id){
        User user = getUser(id);

        return userMapper.toResponse(user);
    }

    // Need authorization check
    public UserResponce update(Long id, UpdateUserRequest request){
        User user = getUser(id);
        user.update(request.firstName(), request.lastName(), request.phone());
        User savedUser = userRepository.saveAndFlush(user);

        return userMapper.toResponse(savedUser);
    }


    // Super Admin




    // Hjälpmetod
    private User getUser(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
