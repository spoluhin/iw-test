package com.example.demo.model;

// Слово, частота использования, позиция в рейтинге
public record WordFrequency(String word, long frequency, int position) {
}
