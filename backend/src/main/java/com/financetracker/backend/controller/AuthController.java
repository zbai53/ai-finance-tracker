package com.financetracker.backend.controller;

import com.financetracker.backend.common.Result;
import com.financetracker.backend.entity.User;
import com.financetracker.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController{
    private final UserService userService;

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request){
        User user = userService.register(
                request.username(),
                request.email(),
                request.password()
        );
        return Result.success(user);
    }

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginRequest request){
        String token = userService.login(request.email(),request.password());
        return Result.success(token);
    }

    record RegisterRequest(
            @jakarta.validation.constraints.NotBlank String username,
            @jakarta.validation.constraints.NotBlank String email,
            @jakarta.validation.constraints.NotBlank String password
    ){}
    record LoginRequest(
            @jakarta.validation.constraints.NotBlank String email,
            @jakarta.validation.constraints.NotBlank String password
    ) {}
}
