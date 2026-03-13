package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleUser;
import com.hoz.hozitech.application.services.AddressService;
import com.hoz.hozitech.domain.dtos.request.AddressRequest;
import com.hoz.hozitech.domain.dtos.response.AddressResponse;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestAPI("${api.prefix-client}/addresses")
@RoleUser
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses() {
        return ResponseEntity.ok(ApiResponse.success("Fetch addresses successfully", addressService.getUserAddresses()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Address created successfully", addressService.createAddress(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", addressService.updateAddress(id, request)));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Address marked as default", addressService.setDefaultAddress(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable UUID id) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok(ApiResponse.success("Address removed successfully"));
    }
}
