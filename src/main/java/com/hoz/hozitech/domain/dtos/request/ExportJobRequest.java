package com.hoz.hozitech.domain.dtos.request;

import com.hoz.hozitech.domain.enums.ExportJobType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobRequest {
    @NotNull(message = "{validation.export_job_type_is_required}")
    private ExportJobType type;

    private Map<String, Object> params;
}
