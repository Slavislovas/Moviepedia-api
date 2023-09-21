package com.api.MoviePedia.controller;

import com.api.MoviePedia.exception.RequestBodyFieldValidationException;
import com.api.MoviePedia.model.ActorCreationDto;
import com.api.MoviePedia.model.ActorRetrievalDto;
import com.api.MoviePedia.model.FieldValidationErrorModel;
import com.api.MoviePedia.service.ActorService;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/actor")
@RestController
public class ActorController {
    private final ActorService actorService;

    @GetMapping("/get/all")
    public ResponseEntity<List<ActorRetrievalDto>> getAllActors(){
        return ResponseEntity.ok(actorService.getAllActors());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<ActorRetrievalDto> getActorById(@PathVariable("id") Long actorId){
        return new ResponseEntity<>(actorService.getActorById(actorId), HttpStatus.FOUND);
    }

    @PostMapping("/create")
    public ResponseEntity<ActorRetrievalDto> createActor(@RequestBody @Valid ActorCreationDto actorCreationDto, BindingResult bindingResult) throws IOException {
        validateRequestBodyFields(bindingResult);
        return new ResponseEntity<>(actorService.createActor(actorCreationDto), HttpStatus.CREATED);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<ActorRetrievalDto> editActorById(@PathVariable("id") Long actorId, @RequestBody @Valid ActorCreationDto actorCreationDto,
                                                           BindingResult bindingResult) throws IOException {
        validateRequestBodyFields(bindingResult);
        return ResponseEntity.ok(actorService.editActorById(actorId, actorCreationDto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteActorById(@PathVariable("id") Long actorId){
        actorService.deleteActorById(actorId);
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
