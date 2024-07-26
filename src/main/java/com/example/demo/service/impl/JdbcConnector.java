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

import static org.springframework.transaction.annotation.Propagation.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JdbcConnector {

    private static final String DELETE_COMMAND =
            """
                    DELETE FROM %s
                    WHERE id IN (%s)
                    """;
    private static final String SELECT_COMMAND =
            """
                    SELECT id FROM %s
                    WHERE col4 < ?
                    """;

    JdbcTemplate jdbcTemplate;

    @Transactional(propagation = REQUIRES_NEW)
    public List<Long> getIds(String tableName, LocalDateTime olderThan) {
        val result = new ArrayList<Long>();
        val command = SELECT_COMMAND.formatted(tableName);
        log.info("Comm: {}", command);
        jdbcTemplate.query(
                command,
                rs -> {
                    result.add(rs.getLong("id"));
                }, olderThan
        );
        return result;
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void batchDelete(String tableName, List<Long> ids, ExecutorService executorService) {
        val strIds = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        val command = DELETE_COMMAND.formatted(tableName, strIds);
        log.info("Start cleaning for {} ids", ids.size());
        executorService.submit(() -> jdbcTemplate.batchUpdate(command));
    }

}
