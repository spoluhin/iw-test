package com.example.demo.controller.doc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Tag(name = "Data Deletion API", description = "API для удаления старых данных из больших таблиц")
public interface DeletionController {

    @Operation(summary = "Запуск процесса удаления старых данных",
               description = "Асинхронно запускает процесс удаления данных старше указанной даты из заданной таблицы")
    @ApiResponse(responseCode = "200", description = "Процесс удаления успешно запущен")
    @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса")
    @ApiResponse(responseCode = "409", description = "Процесс удаления для данной таблицы уже запущен")
    ResponseEntity<String> deleteOldData(
        @Parameter(description = "Имя таблицы для удаления данных", required = true) String tableName,
        @Parameter(description = "Дата и время, старше которых нужно удалить данные", required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime olderThan
    );
}