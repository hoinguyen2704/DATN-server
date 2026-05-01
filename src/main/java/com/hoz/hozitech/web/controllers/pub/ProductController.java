package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.application.services.product.ProductService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestAPI("${api.prefix-client}/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false, defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(required = false, defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDir) {
        PageResponse<ProductResponse> products = productService.searchProducts(
                keyword, categorySlug, brand, page, size, sortBy, sortDir);
        return ResponseEntity.ok(responseFactory.success("response.product.list_fetched", products));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity
                .ok(responseFactory.success("response.product.fetched", productService.getProductBySlug(slug)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getFeaturedProducts(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(responseFactory.success("response.product.featured_fetched",
                productService.getFeaturedProducts(limit)));
    }

    @GetMapping("/new-arrivals")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getNewArrivals(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(responseFactory.success("response.product.new_arrivals_fetched",
                productService.getNewArrivals(limit)));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getTopRatedProducts(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(responseFactory.success("response.product.top_rated_fetched",
                productService.getTopRatedProducts(limit)));
    }
}
