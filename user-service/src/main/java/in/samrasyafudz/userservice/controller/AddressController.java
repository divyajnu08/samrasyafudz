package in.samrasyafudz.userservice.controller;

import in.samrasyafudz.userservice.dto.AddressRequest;
import in.samrasyafudz.userservice.dto.AddressResponse;
import in.samrasyafudz.commonsecurity.AuthenticatedUser;
import in.samrasyafudz.userservice.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/{id}")
    public AddressResponse getOne(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return addressService.getOne(user.userId(), id);
    }

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return addressService.listForUser(user.userId());
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.create(user.userId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(user.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable Long id) {
        addressService.delete(user.userId(), id);
        return ResponseEntity.noContent().build();
    }
}