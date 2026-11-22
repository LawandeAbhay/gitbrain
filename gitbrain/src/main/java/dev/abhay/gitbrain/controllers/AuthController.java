package dev.abhay.gitbrain.controllers;

import dev.abhay.gitbrain.entity.User;
import dev.abhay.gitbrain.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/auth")
public class AuthController {

    private final CurrentUser currentUser;
    public AuthController(CurrentUser currentUser){
        this.currentUser=currentUser;
    }

    @GetMapping("/login-url")
    public Map<String, String> loginURL(){
       Map<String, String> response=Map.of("url", "/oauth2/authorization/github");
        System.out.println(response);
       return response;
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(){
        return ResponseEntity.ok(currentUser.require().getUser());
    }
}

