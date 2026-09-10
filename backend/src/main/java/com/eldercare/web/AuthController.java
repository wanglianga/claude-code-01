package com.eldercare.web;

import com.eldercare.common.ApiException;
import com.eldercare.domain.User;
import com.eldercare.repo.UserRepository;
import com.eldercare.security.JwtService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          UserRepository users, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    @PostMapping("/login")
    public Map<String, Object> login(@org.springframework.web.bind.annotation.RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (Exception e) {
            throw new ApiException("用户名或密码错误");
        }
        User u = users.findByUsername(req.username()).orElseThrow(() -> new ApiException("用户不存在"));
        String token = jwtService.generate(u.getId(), u.getUsername(), u.getRealName(), u.getRole());
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", u.getId());
        userInfo.put("username", u.getUsername());
        userInfo.put("realName", u.getRealName());
        userInfo.put("role", u.getRole());
        userInfo.put("phone", u.getPhone());
        userInfo.put("organization", u.getOrganization());
        return Map.of("token", token, "user", userInfo);
    }

    /** 开发/演示用：校验当前 token 是否有效 */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of("ok", true);
    }
}
