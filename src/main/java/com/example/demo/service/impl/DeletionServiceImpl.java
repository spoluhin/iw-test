package com.example.demo.service.impl;

import com.example.demo.service.*;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.time.*;
import java.util.concurrent.*;

import static java.lang.Math.*;
import static java.lang.Runtime.*;
import static java.util.concurrent.Executors.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeletionServiceImpl implements DeletionService {

    // Отслеживает, есть ли уже запущенный процесс удаления для конкретной таблицы
    ConcurrentHashMap<String, Boolean> activeDeleteTasks = new ConcurrentHashMap<>();

    JdbcConnector jdbcConnector;

    @NonFinal
    @Value("${deletion.batch-size}")
    int batchSize;


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
        val idsToDelete = jdbcConnector.getIds(tableName, olderThan, batchSize);
        try {
            log.info("Load {} ids for deletion, batch size {}", idsToDelete.size(), batchSize);

            for (int i = 0; i < idsToDelete.size(); i += batchSize) {
                val ids = idsToDelete.subList(i, min(i + batchSize, idsToDelete.size()));
                if (ids.isEmpty()) {
                    continue;
                }
                jdbcConnector.batchDelete(tableName, ids);
            }

        } finally {
            activeDeleteTasks.remove(tableName);
        }
    }
}