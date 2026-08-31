package in.samrasyafudz.paymentservice.client;

import in.samrasyafudz.paymentservice.exception.OrderNotAvailableException;
import in.samrasyafudz.paymentservice.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OrderServiceClient {

    private final RestClient restClient;
    private final JwtService jwtService;

    public OrderServiceClient(@Value("${order-service.url}") String orderServiceUrl, JwtService jwtService) {
        this.restClient = RestClient.builder().baseUrl(orderServiceUrl).build();
        this.jwtService = jwtService;
    }

    public OrderDto getOrder(Long orderId) {
        try {
            OrderDto order = restClient.get()
                    .uri("/api/admin/orders/{id}", orderId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateServiceToken())
                    .retrieve()
                    .body(OrderDto.class);

            if (order == null) {
                throw new OrderNotAvailableException(orderId);
            }
            return order;
        } catch (RestClientException e) {
            throw new OrderNotAvailableException(orderId);
        }
    }

    public void confirmOrder(Long orderId) {
        restClient.put()
                .uri("/api/admin/orders/{id}/status", orderId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateServiceToken())
                .body(new StatusUpdateBody("CONFIRMED"))
                .retrieve()
                .toBodilessEntity();
    }

    private record StatusUpdateBody(String status) {
    }
}