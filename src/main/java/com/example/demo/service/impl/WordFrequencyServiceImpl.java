package com.example.demo.service.impl;

import com.example.demo.model.*;
import com.example.demo.service.*;
import com.google.common.hash.*;
import lombok.extern.slf4j.*;
import lombok.*;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.*;

import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.*;
import java.util.stream.*;

import static com.google.common.hash.Funnels.*;
import static java.util.Collections.reverse;
import static java.util.Comparator.comparingLong;
import static java.util.concurrent.CompletableFuture.*;
import static java.util.concurrent.Executors.*;
import static java.util.stream.Collectors.*;

@Slf4j
@Service
public class WordFrequencyServiceImpl implements WordFrequencyService {

    private static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() - 1;
    private static final String FILE_POSTFIX = ".txt";


    // Используется фильтр Блума для уменьшения количества обращений к Map
    private static final int BLOOM_FILTER_EXPECTED_INSERTIONS = 10_000_000;
    private static final double BLOOM_FILTER_FALSE_POSITIVE_PROBABILITY = 0.01;

    // Stealing pool для увеличения производительности
    private final ExecutorService executor = newWorkStealingPool(DEFAULT_THREAD_POOL_SIZE);
    private final BloomFilter<CharSequence> bloomFilter = BloomFilter.create(
            stringFunnel(Charset.defaultCharset()),
            BLOOM_FILTER_EXPECTED_INSERTIONS,
            BLOOM_FILTER_FALSE_POSITIVE_PROBABILITY
    );

    @Override
    @Cacheable(value = "wordFrequencyCache", key = "#folderPath + #minLength + #topCount")
    public Collection<WordFrequency> getTopWords(String folderPath, int minLength, int topCount) {
        log.info("Start word frequency for {} folder with min length {} and top of {} word", folderPath, minLength, topCount);
        val wordFrequency = processFiles(folderPath, minLength);
        return getTopWords(wordFrequency, topCount);
    }

    private Map<String, Long> processFiles(String folderPath, int minLength) {
        val dir = Paths.get(folderPath);

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
                            processFile(file, minLength, wordFrequency), executor))
                    .toList();

            // Дожидаемся завершения обрабокти всех файлов
            allOf(futures.toArray(new CompletableFuture[0]))
                    .join();

            return wordFrequency.entrySet()
                    .stream()
                    .collect(toMap(Map.Entry::getKey, e -> e.getValue().sum()));
        } catch (Exception e) {
            log.error("Error while opening files directory {}, length = {}", folderPath, minLength, e);
            throw new RuntimeException("Error while files processing", e);
        }
    }

    // Обработка конкретного файла построчно
    private void processFile(Path file, int minLength, final ConcurrentHashMap<String, LongAdder> wordFrequency) {
        log.info("Start processing file {}", file);

        try (val lines = Files.lines(file, Charset.defaultCharset())) {
            lines.flatMap(l -> extractWords(l, minLength))
                    .forEach(w -> {
                        try {
                            processWord(w, wordFrequency);
                        } catch (Exception e) {
                            log.error("Error while processing file {}, word {}, error message {}", file, w, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error while files processing {}, length {}", file, minLength, e);
        }
    }

    // Обработка конкретного слова
    private void processWord(String word, ConcurrentHashMap<String, LongAdder> wordFrequency) {
        if (!bloomFilter.mightContain(word)) {
            wordFrequency.computeIfAbsent(word, k -> new LongAdder()).increment();
            bloomFilter.put(word);
        } else {
            wordFrequency.get(word).increment();
        }
    }

    private Collection<WordFrequency> getTopWords(Map<String, Long> wordFrequency, int topCount) {
        PriorityQueue<Map.Entry<String, Long>> minHeap = new PriorityQueue<>(topCount, comparingLong(Map.Entry::getValue));
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
            Map.Entry<String, Long> entry = minHeap.poll();
            topWords.add(new WordFrequency(entry.getKey(), entry.getValue(), topCount - position + 1));
            position++;
        }

        reverse(topWords);
        return topWords;
    }

    private static Stream<String> extractWords(String input, int minLength) {
        val words = new ArrayList<String>();
        val pattern = Pattern.compile("\\b\\p{L}+\\b");  // \\b - граница слова, \\p{L}+ - последовательность букв
        val matcher = pattern.matcher(input);

        while (matcher.find()) {
            words.add(matcher.group().toLowerCase());
        }

        return words.stream()
                .filter(w -> w.length() > minLength);
    }
}