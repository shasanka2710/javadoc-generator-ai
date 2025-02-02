package com.org.javadoc.ai.generator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.org.javadoc.ai.generator.model.SonarIssue;
import com.org.javadoc.ai.generator.model.SonarMetricsModel;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SonarService {

    private Logger logger = LoggerFactory.getLogger(SonarService.class);

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    private final String sonarUrl;

    private final String componentKeys;

    private final String severities;

    private final int pageSize;

    private final String sonarUsername;

    private final String sonarPassword;

    private final MongoTemplate mongoTemplate;

    @Value("${developer.dollarValuePerMinute}")
    double dollarValuePerMinute;

    public SonarService(RestTemplate restTemplate, ObjectMapper objectMapper, @Value("${sonar.url}") String sonarUrl, @Value("${sonar.componentKeys}") String componentKeys, @Value("${sonar.severities}") String severities, @Value("${sonar.pageSize}") int pageSize, @Value("${sonar.username}") String sonarUsername, @Value("${sonar.password}") String sonarPassword, MongoTemplate mongoTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.sonarUrl = sonarUrl;
        this.componentKeys = componentKeys;
        this.severities = severities;
        this.pageSize = pageSize;
        this.sonarUsername = sonarUsername;
        this.sonarPassword = sonarPassword;
        this.mongoTemplate = mongoTemplate;
    }

    public List<SonarIssue> fetchSonarIssues() throws IOException {
        return parseSonarIssues(fetchIssuesFromSonar());
    }

    public String fetchIssuesFromSonar() {
        String url = String.format("%s?componentKeys=%s&severities=%s&resolved=false&ps=%d", sonarUrl, componentKeys, severities, pageSize);
        logger.info("Fetching issues from Sonar: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(sonarUsername, sonarPassword);
            // Disable caching
            headers.setCacheControl(CacheControl.noCache());
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Fetched issues from Sonar successfully.");
                // Async call via injected dependency
                saveIssuesToMongoAsync(response.getBody());
                return response.getBody();
            } else {
                logger.error("Failed to fetch issues from Sonar. Status: {}", response.getStatusCode());
                // Return empty JSON if there's an error
                return "{}";
            }
        } catch (Exception e) {
            logger.error("Error fetching issues from SonarQube: ", e);
            return "{}";
        }
    }

    private List<SonarIssue> parseSonarIssues(String responseBody) throws IOException {
        JsonNode issuesNode = objectMapper.readTree(responseBody).path("issues");
        List<SonarIssue> issues = new ArrayList<>();
        for (JsonNode issueNode : issuesNode) {
            SonarIssue issue = new SonarIssue();
            issue.setKey(issueNode.path("key").asText());
            issue.setType(issueNode.path("type").asText());
            issue.setSeverity(issueNode.path("severity").asText());
            issue.setDescription(issueNode.path("message").asText());
            issue.setCategory(replaceText(issueNode.path("component").asText()));
            // Extract softwareQualities from impacts array
            List<String> softwareQualities = new ArrayList<>();
            for (JsonNode impactNode : issueNode.path("impacts")) {
                softwareQualities.add(impactNode.path("softwareQuality").asText());
            }
            issue.setSoftwareQuality(softwareQualities);
            issues.add(issue);
        }
        return issues;
    }

    private String replaceText(String input) {
        return (input != null) ? (input.replaceFirst("^[^:]+:", "").replace("/", ".")) : input;
    }

    public SonarMetricsModel getSonarMetrics() {
        SonarMetricsModel sonarMetricsModel = new SonarMetricsModel();
        try {
            JsonNode rootNode = getRootNode();
            sonarMetricsModel.setTotalIssuesCount(rootNode.get("total").asInt());
            sonarMetricsModel.setTechDebtTime(rootNode.get("effortTotal").asText());
            sonarMetricsModel.setDollarImpact(rootNode.get("effortTotal").asInt() * dollarValuePerMinute);
        } catch (IOException e) {
            logger.error("Error fetching Sonar metrics: ", e);
        }
        return sonarMetricsModel;
    }

    public JsonNode getRootNode() throws IOException {
        // Returning the expression directly instead of assigning to a temporary variable
        return objectMapper.readTree(fetchIssuesFromSonar());
    }

    @Async
    public void saveIssuesToMongoAsync(String responseBody) {
        saveIssuesToMongo(responseBody);
    }

    private void saveIssuesToMongo(String responseBody) {
        try {
            MongoCollection<Document> collection = mongoTemplate.getCollection("sonarissues");
            List<Document> documents = new ArrayList<>();
            objectMapper.readTree(responseBody).path("issues").forEach(issueNode -> documents.add(Document.parse(issueNode.toString())));
            collection.insertMany(documents);
        } catch (Exception e) {
            logger.error("Handling and suppressing the exception", e);
        }
    }
}