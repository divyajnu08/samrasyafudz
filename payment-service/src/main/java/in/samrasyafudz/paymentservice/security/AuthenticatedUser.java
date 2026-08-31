package in.samrasyafudz.paymentservice.security;

public record AuthenticatedUser(Long userId, String phone, String role) {
}