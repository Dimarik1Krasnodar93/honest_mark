package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi<T>
{
    private Semaphore requestSemaphore;
    private ScheduledExecutorService scheduler;
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

    public CrptApi() {
    }

    public  ResponseEntity<T> post(String address, MediaType mediaType, Map<String, Object> params, Map<String, String> headers) throws InterruptedException, IOException {
        requestSemaphore.acquire();
        RestTemplate<T> restTemplate = new RestTemplate<>();
        return restTemplate.post(address, mediaType, params, headers);

    }

    @NoArgsConstructor
    @Getter
    @Setter
    @AllArgsConstructor
    public static class Doc {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    @AllArgsConstructor
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }

    @Getter
    @NoArgsConstructor
    @Setter
    public static class Description {
        String participantInn;
    }


    private static class RestTemplate<T> {
        ResponseEntity<T> post(String url, MediaType mediaType, Map<String, Object> params, Map<String, String> headers) throws IOException {
            OkHttpClient client = new OkHttpClient();
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(params);
            RequestBody requestBody = RequestBody.create(json, mediaType);
            Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
            Response response = client.newCall(requestBuilder.build()).execute();
            T tMess = new ConverterIml<T>().convert(response.body().string());
            return new ResponseEntity<>(response.code(), tMess);

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
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi<String> crptApi = new CrptApi(TimeUnit.MILLISECONDS, 5);
        String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        String json = "{\"description\": { \"participantInn\": \"string\" }," +
                " \"doc_id\": \"string\", \"doc_status\": \"string\", \"doc_type\": \"LP_INTRODUCE_GOODS\", \"importRequest\": true, \"owner_inn\": \"string\", \"participant_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\", \"products\": [ { \"certificate_document\": \"string\", \"certificate_document_date\": \"2020-01-23\", \"certificate_document_number\": \"string\", \"owner_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"tnved_code\": \"string\", \"uit_code\": \"string\", \"uitu_code\": \"string\" } ], \"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}";
        String signExample = "12345";
        System.out.println("start");
        Map<String, String> headers = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        Doc doc = objectMapper.readValue(json, Doc.class);
        Map<String, Object> params = new HashMap<>();
        params.put("body", doc);
        params.put("sign", signExample);
        ResponseEntity<String> responseEntity =  crptApi.post(url,
                MediaType.parse("application/json"),
                params, headers);
        System.out.println(String.format("Code %s, answer %s", responseEntity.getCode(),
                responseEntity.getBody()));
        System.out.println("end");
    }
}
