package in.samrasyafudz.userservice.service;

import in.samrasyafudz.userservice.dto.AuthResponse;
import in.samrasyafudz.userservice.entity.Role;
import in.samrasyafudz.userservice.entity.User;
import in.samrasyafudz.userservice.repository.UserRepository;
import in.samrasyafudz.commonsecurity.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(OtpService otpService, UserRepository userRepository, JwtService jwtService) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public void sendOtp(String phone) {
        otpService.sendOtp(phone);
    }

    @Transactional
    public AuthResponse verifyOtpAndLogin(String phone, String otp) {
        otpService.verifyOtp(phone, otp);

        boolean isNewUser = !userRepository.existsByPhone(phone);

        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> createNewCustomer(phone));

        String token = jwtService.generateToken(user.getId(), user.getPhone(), user.getRole().name());

        return new AuthResponse(
                token, user.getId(), user.getPhone(), user.getFullName(), user.getRole().name(), isNewUser
        );
    }

    private User createNewCustomer(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setRole(Role.CUSTOMER);
        return userRepository.save(user);
    }
}