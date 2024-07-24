package com.example.demo.service;

import java.time.*;

public interface DeletionService {

    /**
     * Запускает процесс удаления данных из указанной таблицы.
     *
     * @param tableName имя таблицы, из которой нужно удалить данные
     * @param olderThan дата и время, старше которых нужно удалить данные
     * @return строка с сообщением о начале процесса удаления
     * @throws IllegalStateException если процесс удаления для данной таблицы уже запущен
     */
    String startDeletionProcess(String tableName, LocalDateTime olderThan);

}