package com.example.demo.service.impl;

import lombok.*;
import lombok.experimental.*;
import lombok.extern.slf4j.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newWorkStealingPool;
import static org.springframework.transaction.annotation.Propagation.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JdbcConnector {

    private static final int DEFAULT_THREAD_POOL_SIZE = max(getRuntime().availableProcessors() - 1, 1);

    private static final String DELETE_COMMAND =
            """
                    DELETE FROM %s
                    WHERE id IN (%s)
                    """;
    private static final String SELECT_COUNT_COMMAND =
            """
                    SELECT COUNT(*) FROM %s
                    """;
    private static final String SELECT_COMMAND =
            """
                    SELECT id FROM %s
                    WHERE col4 < ?
                    LIMIT %d %s
                    """;

    JdbcTemplate jdbcTemplate;
    ExecutorService executorService = newWorkStealingPool(DEFAULT_THREAD_POOL_SIZE);


    @Transactional(propagation = REQUIRES_NEW)
    public List<Long> getIds(String tableName, LocalDateTime olderThan, int batchSize) {
        List<Long> allIds = new ArrayList<>();

        // Получим общее количество строк в таблице
        int totalRows = jdbcTemplate.queryForObject(
                SELECT_COUNT_COMMAND.formatted(tableName),
                Integer.class
        );

        val futures = IntStream.iterate(0, i -> i < totalRows - batchSize, i -> i + batchSize)
                .peek(offset -> log.info("Start loading ids from {} to {}", offset, offset + batchSize))
                .mapToObj(offset -> runAsync(() ->
                        allIds.addAll(fetchBatchIds(tableName, olderThan, batchSize, offset + batchSize))))
                .toList();


        allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        return allIds;
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void batchDelete(String tableName, List<Long> ids) {
        val strIds = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        val command = DELETE_COMMAND.formatted(tableName, strIds);
        log.info("Start cleaning for {} ids", ids.size());
        executorService.submit(() -> jdbcTemplate.batchUpdate(command));
    }

    private List<Long> fetchBatchIds(String tableName, LocalDateTime olderThan, int batchSize, int offset) {
        val offsetStr =  offset > 0 ? "OFFSET %d".formatted(offset) : "";
        val command = String.format(SELECT_COMMAND, tableName, batchSize, offsetStr);
        val result = new ArrayList<Long>();
        log.info("Command {}", command);

        jdbcTemplate.query(
                command,
                rs -> {
                    result.add(rs.getLong("id"));
                }, olderThan
        );

        log.info("Resulted load {} ids", result.size());
        return result;
    }

}
