package com.abhas.AFFORD_1.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class NumberService {

    private List<Double> numbers = new ArrayList<>();

    public void addNumber(Double number) {
        numbers.add(number);
    }

    public List<Double> getNumbers() {
        return new ArrayList<>(numbers);
    }

    public double getAverage() {
        if (numbers.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (Double number : numbers) {
            sum += number;
        }
        return sum / numbers.size();
    }
}
