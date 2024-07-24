package com.example.demo.controller;

import com.example.demo.controller.doc.*;
import com.example.demo.service.impl.*;
import lombok.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;

import static org.springframework.http.ResponseEntity.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/tables")
public class DeletionControllerImpl implements DeletionController {

    private final DeletionServiceImpl deletionServiceImpl;

    @Override
    @DeleteMapping("/{tableName}")
    public ResponseEntity<String> deleteOldData(@PathVariable String tableName,
                                                @RequestParam LocalDateTime olderThan) {
        val result = deletionServiceImpl.startDeletionProcess(tableName, olderThan);
        return ok(result);
    }
}