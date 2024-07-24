package com.example.demo.service;

import com.example.demo.model.*;

import java.util.*;


public interface WordFrequencyService {

    /**
     * Показывает Топ-N слов больше выбранной длины для файлов в указанной директории
     *
     * @param folderPath путь к директории с файлами для анализа
     * @param minLength минимальная длина слова
     * @param topCount количество слов в топе
     * @return список из объектов вида {слово, частота, место в рейтинге}
     */
    Collection<WordFrequency> getTopWords(String folderPath, int minLength, int topCount);
}