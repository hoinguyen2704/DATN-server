package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.application.services.BrandService;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.BrandResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestAPI("${api.prefix-client}/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponse.success("Fetch brands successfully", brandService.getAllBrands()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success("Fetch brand successfully", brandService.getBrandBySlug(slug)));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Fetch brand successfully", brandService.getBrandById(id)));
    }
}
