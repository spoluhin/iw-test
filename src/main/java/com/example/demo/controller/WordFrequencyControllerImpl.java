package com.example.demo.controller;

import com.example.demo.controller.doc.*;
import com.example.demo.model.*;
import com.example.demo.service.*;
import lombok.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static org.springframework.http.ResponseEntity.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
public class WordFrequencyControllerImpl implements WordFrequencyController {

    private final WordFrequencyService wordFrequencyService;

    @Override
    @GetMapping("/words/frequency")
    public ResponseEntity<Collection<WordFrequency>> getTopWords(@RequestParam String folderPath,
                                                                 @RequestParam int minLength,
                                                                 @RequestParam(defaultValue = "10") int topCount) {
        return ok(wordFrequencyService.getTopWords(folderPath, minLength, topCount));
    }
}