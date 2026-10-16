package org.example.homnayangi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.homnayangi.entity.Place;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FoursquareService {

    final ObjectMapper objectMapper;

    @Value("${foursquare.api.key}")
    String apiKey;

    static final String FOURSQUARE_API_URL = "https://places-api.foursquare.com/places";
    static final String API_VERSION = "2025-06-17";

    public List<Place> fetchNearbyRestaurants(double latitude, double longitude, double radiusKm, int limit) {
        int calculatedRadius = (int) (radiusKm * 1000);
        final int finalRadiusInMeters = calculatedRadius <= 0 ? 5000 : calculatedRadius;
        final String llParam = String.format(Locale.US, "%.6f,%.6f", latitude, longitude);

        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl(FOURSQUARE_API_URL)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("X-Places-Api-Version", API_VERSION)
                    .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("ll", llParam)
                            .queryParam("radius", finalRadiusInMeters)
                            .queryParam("categories", "13000") // 13000 là danh mục Dining and Drinking (Quán ăn, Nhà hàng, Cafe)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results");

            List<Place> places = new ArrayList<>();
            for (JsonNode item : results) {
                String name = item.path("name").asText(null);
                if (name == null || name.isBlank()) {
                    continue;
                }

                double lat = item.path("latitude").asDouble();
                double lon = item.path("longitude").asDouble();

                // Lấy category từ categories[0]
                String category = "Quán ăn";
                JsonNode categories = item.path("categories");
                if (categories.isArray() && !categories.isEmpty()) {
                    String catName = categories.get(0).path("name").asText(null);
                    if (catName != null && !catName.isBlank()) {
                        category = catName;
                    }
                }

                // Lấy địa chỉ định dạng từ location.formatted_address
                JsonNode location = item.path("location");
                String formattedAddress = location.path("formatted_address").asText(null);
                if (formattedAddress == null || formattedAddress.isBlank()) {
                    formattedAddress = location.path("address").asText(null);
                }
                if (formattedAddress == null || formattedAddress.isBlank()) {
                    formattedAddress = String.format(Locale.US, "Tọa độ: %.4f, %.4f", lat, lon);
                }

                Place place = Place.builder()
                        .name(name.length() > 255 ? name.substring(0, 255) : name)
                        .category(category.length() > 100 ? category.substring(0, 100) : category)
                        .priceRange(2)
                        .address(formattedAddress)
                        .latitude(BigDecimal.valueOf(lat).setScale(8, RoundingMode.HALF_UP))
                        .longitude(BigDecimal.valueOf(lon).setScale(8, RoundingMode.HALF_UP))
                        .imageUrl(null)
                        .build();

                places.add(place);
            }

            log.info("Lấy thành công {} quán ăn từ Foursquare Places API", places.size());
            return places;

        } catch (Exception e) {
            log.warn("Không thể lấy quán ăn từ Foursquare Places API: {}", e.getMessage());
            return List.of();
        }
    }
}
