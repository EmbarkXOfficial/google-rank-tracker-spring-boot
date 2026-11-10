package com.demo.ranktracker;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class SearchApiClient {
    private final RestClient restClient;

    public SearchApiClient(RestClient.Builder builder,
                           @Value("${search.api-key}") String API_KEY) {
        this.restClient = builder
                .baseUrl("https://www.searchapi.io/api/v1/search")
                .defaultHeader("Authorization", API_KEY)
                .build();

    }

    public List<Result> top100(String keyword) {
        Response response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("engine", "google_rank_tracking")
                        .queryParam("q", keyword)
                        .build())
                .retrieve()
                .body(Response.class);
        return response.organicResults() == null ? List.of()
                : response.organicResults();
    }

    record Response(@JsonProperty("organic_results") List<Result> organicResults) {
    }

    record Result(int position, String link, String domain) {
    }
}
