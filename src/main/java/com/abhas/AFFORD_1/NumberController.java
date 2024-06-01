package com.abhas.AFFORD_1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.abhas.AFFORD_1.service.NumberService;


import java.util.*;

@RestController
@RequestMapping("/numbers")
public class NumberController {

    private final String testServerUrl = "http://20.244.56.144/test/primes"; // Replace with the actual test server URL
    private final int windowSize = 10; // Define the window size for stored numbers
    private final int timeout = 500; // Timeout in milliseconds

    @Autowired
    private NumberService numberService;

    private Queue<Double> numberCache = new LinkedList<>();
    private Set<Double> uniqueNumbers = new HashSet<>();

    @PostMapping("/{numberId}")
    public ResponseEntity<Map<String, Object>> addNumber(@PathVariable String numberId) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> response = new HashMap<>();
        if (!isValidId(numberId)) {
            response.put("error", "Invalid number ID.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (numberCache.isEmpty() || numberCache.size() < windowSize) {
            fetchNumbersFromServer();
        }
        if (!numberCache.isEmpty()) {
            Double number = numberCache.poll();
            if (!uniqueNumbers.contains(number)) {
                numberService.addNumber(number);
                uniqueNumbers.add(number);
                if (numberCache.size() >= windowSize) {
                    numberCache.poll(); // Remove the oldest number from the cache
                    uniqueNumbers.remove(number);
                }
                response.put("message", "Number added successfully.");
            } else {
                response.put("message", "Duplicate number found, skipping.");
            }
        } else {
            response.put("error", "No more numbers available.");
        }
        List<Double> numbersReceived = numberService.getNumbers();
        response.put("numbers", numbersReceived);
        response.put("windowPrevState", new ArrayList<>()); // Assuming no previous state initially
        response.put("windowCurrState", numbersReceived);
        response.put("avg", String.format("%.2f", numberService.getAverage()));

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        if (processingTime > timeout) {
            response.put("error", "Request processing time exceeded 500 milliseconds.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{numberId}")
    public ResponseEntity<Map<String, Object>> getAverage(@PathVariable String numberId) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> response = new HashMap<>();
        if (!isValidId(numberId)) {
            response.put("error", "Invalid number ID.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        List<Double> numbersReceived = numberService.getNumbers();
        response.put("numbers", numbersReceived);
        response.put("windowPrevState", new ArrayList<>()); // Assuming no previous state initially
        response.put("windowCurrState", numbersReceived);
        response.put("avg", String.format("%.2f", numberService.getAverage()));

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        if (processingTime > timeout) {
            response.put("error", "Request processing time exceeded 500 milliseconds.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private boolean isValidId(String numberId) {
        return numberId.length() >= 2 && (numberId.charAt(0) == 'p' || numberId.charAt(0) == 'f' ||
                numberId.charAt(0) == 'e' || numberId.charAt(0) == 'r');
    }

    private void fetchNumbersFromServer() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Double[]> response = restTemplate.exchange(
                    testServerUrl,
                    HttpMethod.GET,
                    entity,
                    Double[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                for (Double number : response.getBody()) {
                    if (!uniqueNumbers.contains(number)) {
                        numberCache.offer(number);
                        uniqueNumbers.add(number);
                    }
                }
            }
        } catch (RestClientException e) {
            // Handle timeout or error scenarios
            System.err.println("Error fetching numbers from the server: " + e.getMessage());
        }
    }
}