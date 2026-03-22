package com.hoz.hozitech.application.services.address;

import com.hoz.hozitech.domain.dtos.request.AddressRequest;
import com.hoz.hozitech.domain.dtos.response.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface AddressService {
    List<AddressResponse> getUserAddresses();

    AddressResponse createAddress(AddressRequest request);

    AddressResponse updateAddress(UUID addressId, AddressRequest request);

    void deleteAddress(UUID addressId);

    AddressResponse setDefaultAddress(UUID addressId);
}
