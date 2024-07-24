package com.example.demo.service.impl;

import com.example.demo.model.*;
import com.example.demo.service.*;
import lombok.experimental.*;
import lombok.extern.slf4j.*;
import lombok.*;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.*;

import java.nio.file.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import java.util.stream.*;

import static java.lang.Math.max;
import static java.lang.Runtime.*;
import static java.nio.charset.Charset.*;
import static java.util.Collections.*;
import static java.util.Comparator.*;
import static java.util.concurrent.CompletableFuture.*;
import static java.util.concurrent.Executors.*;
import static java.util.stream.Collectors.*;
import static lombok.AccessLevel.*;

@Slf4j
@Service
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class WordFrequencyServiceImpl implements WordFrequencyService {

    private static final int DEFAULT_THREAD_POOL_SIZE = max(getRuntime().availableProcessors() - 1, 1);
    private static final String FILE_POSTFIX = ".txt";

    // \\b - граница слова, \\w{%d,} - последовательность букв, где %d длина слова
    private static final String PATTERN_BASE = "\\b\\w{%d,}\\b";

    // Stealing pool для увеличения производительности
    ExecutorService executor = newWorkStealingPool(DEFAULT_THREAD_POOL_SIZE);

    @Override
    @Cacheable(value = "wordFrequencyCache", key = "#folderPath + #minLength + #topCount")
    public Collection<WordFrequency> getTopWords(String folderPath, int minLength, int topCount) {
        log.info("Start word frequency for {} folder with min length {} and top of {} word", folderPath, minLength, topCount);
        val wordFrequency = processFiles(folderPath, minLength);
        return getTopWords(wordFrequency, topCount);
    }

    private Map<String, Long> processFiles(String folderPath, int minLength) {
        val dir = Paths.get(folderPath);
        val pattern_str = PATTERN_BASE.formatted(minLength + 1);
        val pattern = Pattern.compile(pattern_str);
        // Обрабатываем все файлы в том числе из вложенных директорий
        try (val files = Files.walk(dir)) {
            val resultedFiles = files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(FILE_POSTFIX))
                    .toList();

            log.info("Start processing files in {} folder, resulted {} files", folderPath, resultedFiles.size());

            // LongAdder для увеличения производительности
            val wordFrequency = new ConcurrentHashMap<String, LongAdder>();

            // Запускаем для обработки каждого отдельного файла отдельный поток Completable Future
            val futures = resultedFiles.stream()
                    .map(file -> runAsync(() ->
                            processFile(file, wordFrequency, pattern), executor))
                    .toList();

            // Дожидаемся завершения обрабокти всех файлов
            allOf(futures.toArray(new CompletableFuture[0]))
                    .join();

            return wordFrequency.entrySet()
                    .stream()
                    .collect(toMap(Entry::getKey, e -> e.getValue().sum()));
        } catch (Exception e) {
            log.error("Error while opening files directory {}, length = {}", folderPath, minLength, e);
            throw new RuntimeException("Error while files processing", e);
        }
    }

    // Обработка конкретного файла построчно
    private void processFile(Path file, final ConcurrentHashMap<String, LongAdder> wordFrequency, Pattern pattern) {
        log.info("Start processing file {}", file);

        try (val lines = Files.lines(file, defaultCharset())) {
            lines.flatMap(l -> extractWords(l, pattern))
                    .forEach(w -> {
                        try {
                            processWord(w, wordFrequency);
                        } catch (Exception e) {
                            log.error("Error while processing file {}, word {}, error message {}", file, w, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error while files processing {}", file, e);
        }
    }

    // Обработка конкретного слова
    private void processWord(String word, ConcurrentHashMap<String, LongAdder> wordFrequency) {
        wordFrequency.computeIfAbsent(word, k -> new LongAdder()).increment();
    }

    private Collection<WordFrequency> getTopWords(Map<String, Long> wordFrequency, int topCount) {
        val minHeap = new PriorityQueue<Entry<String, Long>>(topCount,
                comparingLong(Entry::getValue));
        for (val entry : wordFrequency.entrySet()) {
            val lastTop = minHeap.peek();
            if (minHeap.size() < topCount) {
                minHeap.offer(entry);
            } else if (lastTop != null && entry.getValue() > lastTop.getValue()) {
                minHeap.poll();
                minHeap.offer(entry);
            }
        }

        val topWords = new ArrayList<WordFrequency>();
        int position = 1;
        while (!minHeap.isEmpty()) {
            Entry<String, Long> entry = minHeap.poll();
            topWords.add(new WordFrequency(entry.getKey(), entry.getValue(), topCount - position + 1));
            position++;
        }

        reverse(topWords);
        return topWords;
    }

    private static Stream<String> extractWords(String input, Pattern pattern) {
        val words = new ArrayList<String>();
        val matcher = pattern.matcher(input);

        while (matcher.find()) {
            words.add(matcher.group().toLowerCase());
        }

        return words.stream();
    }
}