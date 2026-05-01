package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.application.services.brand.BrandService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
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
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {
        return ResponseEntity.ok(responseFactory.success("response.brand.list_fetched", brandService.getAllBrands()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(responseFactory.success("response.brand.fetched", brandService.getBrandBySlug(slug)));
    }
}
