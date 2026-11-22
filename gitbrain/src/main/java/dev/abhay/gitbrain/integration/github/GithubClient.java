package dev.abhay.gitbrain.integration.github;

import dev.abhay.gitbrain.integration.github.dto.GithubRepositoryResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class GithubClient {

    private static final ParameterizedTypeReference<List<GithubRepositoryResponse>> LIST_REPO =
            new ParameterizedTypeReference<>() {};

    private static final String API_VERSION = "2022-11-28";
    private static final int PER_PAGE = 100;

    private final RestClient restClient;

    public GithubClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.github.com")
                .build();
    }

    public List<GithubRepositoryResponse> listUserRepos(String accessToken) {
        List<GithubRepositoryResponse> all = new ArrayList<>();
        int page = 1;

        while (true) {
            final int currentPage = page;

            List<GithubRepositoryResponse> pageRepos = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/user/repos")
                            .queryParam("affiliation", "owner")
                            .queryParam("sort", "updated")
                            .queryParam("per_page", PER_PAGE)
                            .queryParam("page", currentPage)
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", API_VERSION)
                    .retrieve()
                    .body(LIST_REPO);

            if (pageRepos == null || pageRepos.isEmpty()) {
                break;
            }

            all.addAll(pageRepos);

            if (pageRepos.size() < PER_PAGE) {
                break;
            }
            page++;
        }

        return all;
    }
}