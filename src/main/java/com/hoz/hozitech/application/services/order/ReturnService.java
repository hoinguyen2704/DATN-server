package com.hoz.hozitech.application.services.order;

import com.hoz.hozitech.domain.dtos.request.CreateReturnRequest;
import com.hoz.hozitech.domain.dtos.request.ProcessRefundRequest;
import com.hoz.hozitech.domain.dtos.request.ReviewReturnRequest;
import com.hoz.hozitech.domain.dtos.request.UpdateReturnStatusRequest;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ReturnRequestResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ReturnService {

    ReturnRequestResponse createReturnRequest(
            UUID userId,
            CreateReturnRequest request,
            List<MultipartFile> evidenceFiles,
            String idempotencyKey);

    PageResponse<ReturnRequestResponse> getMyReturnRequests(UUID userId, String status, String keyword, int page, int size);

    ReturnRequestResponse getReturnByNumberForUser(UUID userId, String returnNumber);

    ReturnRequestResponse cancelReturnRequest(UUID userId, UUID returnRequestId);

    PageResponse<ReturnRequestResponse> getAllReturnRequests(String status, String keyword, int page, int size);

    ReturnRequestResponse getReturnByNumberForAdmin(String returnNumber);

    ReturnRequestResponse reviewReturnRequest(UUID returnRequestId, ReviewReturnRequest request);

    ReturnRequestResponse updateReturnStatus(UUID returnRequestId, UpdateReturnStatusRequest request);

    ReturnRequestResponse processRefund(UUID returnRequestId, ProcessRefundRequest request, String idempotencyKey);
}
