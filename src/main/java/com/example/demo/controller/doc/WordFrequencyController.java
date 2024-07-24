package com.example.demo.controller.doc;

import com.example.demo.model.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.*;
import org.springframework.http.*;

import java.util.*;

@Tag(name = "API частоты слов", description = "API для анализа частоты слов в текстовых файлах")
public interface WordFrequencyController {

    @Operation(summary = "Получить наиболее часто встречающиеся слова",
            description = "Извлекает наиболее часто встречающиеся слова из текстовых файлов в указанной папке")
    @ApiResponse(responseCode = "200", description = "Операция выполнена успешно",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Map.class)))
    @ApiResponse(responseCode = "400", description = "Неверный ввод")
    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    ResponseEntity<Collection<WordFrequency>> getTopWords(
            @Parameter(description = "Путь к папке, содержащей текстовые файлы", required = true)
            String folderPath,
            @Parameter(description = "Минимальная длина слов для рассмотрения", required = true)
            int minLength,
            @Parameter(description = "Количество наиболее частых слов для возврата", required = false)
            int topCount);
}