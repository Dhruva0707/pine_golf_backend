package com.pinewoods.score.tracker.utilities;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

public class HttpUtilities {

    // Utility method to send HTTP requests
    public static ResponseEntity<String> sendRequest(String path, String jsonBody, String username, String password,
        HttpMethod method, RestClient restClient) {
        try {
            String authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

            // Create a generic request builder that can handle any HTTP method
            RestClient.RequestHeadersSpec<?> requestSpec;
            if (method.equals(HttpMethod.POST)) {
                requestSpec = restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(jsonBody);
            } else if (method.equals(HttpMethod.PUT)) {
                requestSpec = restClient.put().uri(path).contentType(MediaType.APPLICATION_JSON).body(jsonBody);
            } else if (method.equals(HttpMethod.GET)) {
                requestSpec = restClient.get().uri(path);
            } else if (method.equals(HttpMethod.DELETE)) {
                requestSpec = restClient.delete().uri(path);
            } else {
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

            return requestSpec
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .toEntity(String.class);

        } catch (HttpClientErrorException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
