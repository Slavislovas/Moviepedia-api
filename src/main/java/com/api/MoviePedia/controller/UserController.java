package com.api.MoviePedia.controller;

import com.api.MoviePedia.exception.RequestBodyFieldValidationException;
import com.api.MoviePedia.model.FieldValidationErrorModel;
import com.api.MoviePedia.model.UserCreationDto;
import com.api.MoviePedia.model.UserEditDto;
import com.api.MoviePedia.model.UserRetrievalDto;
import com.api.MoviePedia.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/user")
@RequiredArgsConstructor
@RestController
public class UserController {
    private final UserService userService;

    @GetMapping("/get/all")
    public ResponseEntity<List<UserRetrievalDto>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }
    @GetMapping("/get/{id}")
    public ResponseEntity<UserRetrievalDto> getUserById(@PathVariable("id") Long userId){
        return new ResponseEntity<>(userService.getUserById(userId), HttpStatus.FOUND);
    }

    @PostMapping("/register")
    public ResponseEntity<UserRetrievalDto> registerUser(@RequestBody @Valid UserCreationDto creationDto, BindingResult bindingResult){
        validateRequestBodyFields(bindingResult);
        return new ResponseEntity<>(userService.registerUser(creationDto), HttpStatus.CREATED);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<UserRetrievalDto> editUserById(@PathVariable("id") Long userId, @RequestBody @Valid UserEditDto editDto, BindingResult bindingResult){
        validateRequestBodyFields(bindingResult);
        return ResponseEntity.ok(userService.editUserById(editDto, userId));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable("id") Long userId){
        userService.deleteUserById(userId);
        return ResponseEntity.ok().build();
    }

    private void validateRequestBodyFields(BindingResult bindingResult) {
        List<FieldValidationErrorModel> fieldValidationErrors = new ArrayList<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            fieldValidationErrors.add(new FieldValidationErrorModel(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        if (!fieldValidationErrors.isEmpty()){
            throw new RequestBodyFieldValidationException(fieldValidationErrors);
        }
    }
}
