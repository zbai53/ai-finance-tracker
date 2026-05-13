package com.financetracker.backend.service;

import com.financetracker.backend.entity.User;
import com.financetracker.backend.mapper.UserMapper;
import com.financetracker.backend.security.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor

public class UserService{
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public User register(String username, String email, String password){
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        if(userMapper.selectOne(wrapper) != null){
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();

        userMapper.insert(user);
        log.info("New user registered: {}", email);
        return user;
    }

    public String login(String email, String password){
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("User not found");
        }


        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }


        return jwtUtil.generateToken(user.getId());
    }
}
