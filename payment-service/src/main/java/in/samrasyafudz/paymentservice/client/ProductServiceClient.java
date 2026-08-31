package in.samrasyafudz.paymentservice.client;

import in.samrasyafudz.paymentservice.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductServiceClient {

    private final RestClient restClient;
    private final JwtService jwtService;

    public ProductServiceClient(@Value("${product-service.url}") String productServiceUrl, JwtService jwtService) {
        this.restClient = RestClient.builder().baseUrl(productServiceUrl).build();
        this.jwtService = jwtService;
    }

    public void deductStock(Long variantId, Integer quantity) {
        restClient.post()
                .uri("/api/admin/products/variants/{variantId}/deduct-stock", variantId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateServiceToken())
                .body(new DeductStockBody(quantity))
                .retrieve()
                .toBodilessEntity();
    }

    private record DeductStockBody(Integer quantity) {
    }
}