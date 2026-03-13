package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.repositories.AddressRepository;
import com.hoz.hozitech.application.services.AddressService;
import com.hoz.hozitech.application.services.UserService;
import com.hoz.hozitech.domain.dtos.request.AddressRequest;
import com.hoz.hozitech.domain.dtos.response.AddressResponse;
import com.hoz.hozitech.domain.entities.Address;
import com.hoz.hozitech.domain.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserService userService;

    @Override
    public List<AddressResponse> getUserAddresses() {
        User user = userService.getCurrentUserEntity();
        List<Address> addresses = addressRepository.findByUserIdOrderByIsDefaultDesc(user.getId());
        return addresses.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse createAddress(AddressRequest request) {
        User user = userService.getCurrentUserEntity();

        // If this is their first address, or they checked isDefault, set it as default
        boolean shouldBeDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (addressRepository.countByUserId(user.getId()) == 0) {
            shouldBeDefault = true;
        }

        if (shouldBeDefault) {
            clearDefaultAddresses(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .detailAddress(request.getDetailAddress())
                .isDefault(shouldBeDefault)
                .build();

        return mapToResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID addressId, AddressRequest request) {
        Address address = getAddressIfBelongsToUser(addressId);

        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setDetailAddress(request.getDetailAddress());

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefaultAddresses(address.getUser().getId());
            address.setIsDefault(true);
        }

        return mapToResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(UUID addressId) {
        Address address = getAddressIfBelongsToUser(addressId);

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            throw new IllegalArgumentException("Cannot delete default address");
        }

        addressRepository.delete(address);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID addressId) {
        Address address = getAddressIfBelongsToUser(addressId);

        if (!Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefaultAddresses(address.getUser().getId());
            address.setIsDefault(true);
            return mapToResponse(addressRepository.save(address));
        }

        return mapToResponse(address);
    }

    private Address getAddressIfBelongsToUser(UUID addressId) {
        User user = userService.getCurrentUserEntity();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You don't have permission to modify this address");
        }
        return address;
    }

    private void clearDefaultAddresses(UUID userId) {
        List<Address> existingDefaults = addressRepository.findByUserIdAndIsDefaultTrue(userId);
        for (Address addr : existingDefaults) {
            addr.setIsDefault(false);
            addressRepository.save(addr);
        }
    }

    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .detailAddress(address.getDetailAddress())
                .isDefault(address.getIsDefault())
                .build();
    }
}
