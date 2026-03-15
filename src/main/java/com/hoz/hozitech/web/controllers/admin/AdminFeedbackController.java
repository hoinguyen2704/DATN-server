package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import com.hoz.hozitech.application.services.ExportService;
import com.hoz.hozitech.application.services.FeedbackService;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.FeedbackResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestAPI("${api.prefix-admin}/feedbacks")
@RoleAdmin
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;
    private final ExportService exportService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeedbackResponse>>> getAllFeedbacks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Fetch feedbacks successfully",
                feedbackService.getAllFeedbacks(status, productId, page, size)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(ApiResponse.success("Feedback status updated",
                feedbackService.updateFeedbackStatus(id, request.get("status"))));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<ApiResponse<FeedbackResponse>> replyToFeedback(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(ApiResponse.success("Reply sent successfully",
                feedbackService.adminReplyFeedback(id, request.get("reply"))));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportFeedbacks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID productId) {
        byte[] data = exportService.exportFeedbacksToExcel(status, productId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=feedbacks.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
