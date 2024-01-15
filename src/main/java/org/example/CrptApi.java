package org.example;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.MarshalledObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi<T>
{
    private final Semaphore requestSemaphore;
    private final ScheduledExecutorService scheduler;
    private TimeUnit timeUnit;
    private int requestLimit;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        requestSemaphore = new Semaphore(requestLimit);
        scheduler = Executors.newScheduledThreadPool(1);
        long period = timeUnit.toNanos(1);
        scheduler.scheduleAtFixedRate(() -> requestSemaphore.release(), 1, 1, timeUnit);
    }


    public  ResponseEntity<T> post(String address, String json, Map<String, String> headers) throws InterruptedException, IOException {
        requestSemaphore.acquire();
        RestTemplate<T> restTemplate = new RestTemplate<>();
        return restTemplate.post(address, json, headers);

    }


    private static class RestTemplate<T> {
        ResponseEntity<T> post(String urlString, String json, Map<String, String> headers) throws IOException {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                int responseCode = connection.getResponseCode();
                String error = "";
                String message = "";
                if (responseCode >= 200 && responseCode < 300) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        message += line;
                    }
                    reader.close();
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        error += line;
                    }
                    errorReader.close();
                }
                T tMess = new ConverterIml<T>().convert(message);
                return new ResponseEntity<T>(responseCode, tMess, error);
            }
        }

    }

    private interface Converter<T> {
        T convert(String message);
    }

    private static class ConverterIml<T> implements Converter {
        @Override
        public T convert(String message) {
            if (message instanceof String) {
                return (T) message;
            }
            return null;
        }
    }
    @Getter
    @AllArgsConstructor
    private static class ResponseEntity<T> {
        private int code;
        private T body;
        private String error;


    }
    public static void main(String[] args ) throws IOException, InterruptedException {
        String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        String json = "{ \"participantInn\": \"string\" }, \"doc_id\": \"string\", \"doc_status\": \"string\", \"doc_type\": \"LP_INTRODUCE_GOODS\", 109 \"importRequest\": true, \"owner_inn\": \"string\", \"participant_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\", \"products\": [ { \"certificate_document\": \"string\", \"certificate_document_date\": \"2020-01-23\", \"certificate_document_number\": \"string\", \"owner_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"tnved_code\": \"string\", \"uit_code\": \"string\", \"uitu_code\": \"string\" } ], \"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}";
        System.out.println("start");
        CrptApi<String> crptApi = new CrptApi<>(TimeUnit.MILLISECONDS, 5);
        Map<String, String> headers = new HashMap<>();
        ResponseEntity<String> responseEntity = crptApi.post(url, json, headers);
        System.out.println(String.format("Code %s, answer %s, error %s", responseEntity.getCode(),
                responseEntity.getBody(), responseEntity.getError()));
        System.out.println("end");
    }
}
