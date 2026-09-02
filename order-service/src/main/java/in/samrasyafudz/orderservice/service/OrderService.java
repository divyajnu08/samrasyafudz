package in.samrasyafudz.orderservice.service;

import in.samrasyafudz.orderservice.client.UserServiceClient;
import in.samrasyafudz.orderservice.dto.CheckoutRequest;
import in.samrasyafudz.orderservice.dto.OrderItemResponse;
import in.samrasyafudz.orderservice.dto.OrderResponse;
import in.samrasyafudz.orderservice.entity.CartItem;
import in.samrasyafudz.orderservice.entity.Order;
import in.samrasyafudz.orderservice.entity.OrderItem;
import in.samrasyafudz.orderservice.entity.OrderStatus;
import in.samrasyafudz.orderservice.exception.EmptyCartException;
import in.samrasyafudz.orderservice.exception.InvalidOrderStatusTransitionException;
import in.samrasyafudz.orderservice.exception.OrderNotFoundException;
import in.samrasyafudz.orderservice.repository.CartItemRepository;
import in.samrasyafudz.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserServiceClient userServiceClient;

    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository, UserServiceClient userServiceClient) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request, String authHeader) {
        userServiceClient.getAddress(request.getAddressId(), authHeader);

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setAddressId(request.getAddressId());

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setVariantId(cartItem.getVariantId());
            orderItem.setProductName(cartItem.getProductName());
            orderItem.setWeightGrams(cartItem.getWeightGrams());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setQuantity(cartItem.getQuantity());

            BigDecimal subtotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            orderItem.setSubtotal(subtotal);
            total = total.add(subtotal);

            order.getItems().add(orderItem);
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        cartItemRepository.deleteByUserId(userId);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(OrderNotFoundException::new);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidOrderStatusTransitionException(order.getStatus(), newStatus);
        }

        order.setStatus(newStatus);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new InvalidOrderStatusTransitionException(order.getStatus(), OrderStatus.CANCELLED);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getProductId(), i.getVariantId(), i.getProductName(),
                        i.getWeightGrams(), i.getUnitPrice(), i.getQuantity(), i.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(), order.getAddressId(), order.getStatus().name(),
                order.getTotalAmount(), items, order.getCreatedAt()
        );
    }
}