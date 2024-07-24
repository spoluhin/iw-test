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

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JdbcConnector {

    private static final String DELETE_COMMAND =
            """
                    DELETE FROM %s
                    WHERE id IN (?)
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
        jdbcTemplate.query(
                SELECT_COMMAND.formatted(tableName),
                rs -> {
                    result.add(rs.getLong("id"));
                }, olderThan
        );
        return result;
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void batchDelete(String tableName, List<Long> ids, ExecutorService executorService) {
        log.info("Start cleaning for {} ids", ids.size());
        executorService.submit(() -> jdbcTemplate.batchUpdate(
                DELETE_COMMAND.formatted(tableName)
        ), ids);
    }

}
