package com.example.demo.service.impl;

import com.example.demo.service.*;
import jakarta.annotation.*;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.slf4j.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.Math.*;
import static java.lang.Runtime.*;
import static java.util.concurrent.Executors.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeletionServiceImpl implements DeletionService {

    private static final int DEFAULT_THREAD_POOL_SIZE = max(getRuntime().availableProcessors() - 1, 1);

    // Отслеживает, есть ли уже запущенный процесс удаления для конкретной таблицы
    ConcurrentHashMap<String, Boolean> activeDeleteTasks = new ConcurrentHashMap<>();

    JdbcTemplate jdbcTemplate;

    @NonFinal
    ExecutorService executorService;

    @PostConstruct
    public void post() {
        this.executorService = newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
    }

    @Transactional
    public String startDeletionProcess(String tableName, LocalDateTime olderThan) {
        log.info("Start cleaning for {} table by date {}", tableName, olderThan);
        if (activeDeleteTasks.putIfAbsent(tableName, true) == null) {
            performOptimizedDeletion(tableName, olderThan);
            return "Deletion process started for table: " + tableName;
        } else {
            throw new IllegalStateException("Deletion process already running for table: " + tableName);
        }
    }

    // Получаем список ID для удаления из таблицы,
    // делим его на доступное количество потоков
    // и пачками чистим таблицу в разных потоках
    public void performOptimizedDeletion(String tableName, LocalDateTime olderThan) {
        val idsToDelete = new ArrayList<>();
        try {
            jdbcTemplate.query(
                    "SELECT id FROM " + tableName +
                            " WHERE col4 < ? ",
                    rs -> {
                        idsToDelete.add(rs.getLong("id"));
                    },
                    olderThan
            );

            log.info("Load {} ids for deletion", idsToDelete.size());

            int batch = idsToDelete.size() / DEFAULT_THREAD_POOL_SIZE;

            log.info("Start cleaning by {} batch size", batch);
            for (int i = 0; i < idsToDelete.size(); i += batch) {
                val ids = idsToDelete.subList(i, Math.min(i + batch, idsToDelete.size()));
                if (ids.isEmpty()) {
                    continue;
                }
                log.info("Start cleaning for {} ids", ids.size());
                executorService.submit(() -> jdbcTemplate.batchUpdate(
                        "DELETE FROM " + tableName + " WHERE id IN (?)",
                        ids.stream()
                                .map(id -> new Object[]{id}).toList()
                ));
            }

        } finally {
            activeDeleteTasks.remove(tableName);
        }
    }
}