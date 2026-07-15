package com.memoraai.revision.controller;

import com.memoraai.common.response.ApiResponse;
import com.memoraai.revision.dto.RevisionPlanDto;
import com.memoraai.revision.service.RevisionPlannerService;
import com.memoraai.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/revision")
@RequiredArgsConstructor
@Tag(name = "Revision Planner", description = "Daily revision queue APIs")
@SecurityRequirement(name = "bearerAuth")
public class RevisionController {

    private final RevisionPlannerService revisionPlannerService;

    @Operation(
            summary = "Get today's revision plan",
            description = "Returns a priority-ordered list of concepts to review today. " +
                          "Includes concepts where memoryScore < 0.80 or nextReviewAt <= now."
    )
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<RevisionPlanDto>> getTodaysRevisionPlan(
            @AuthenticationPrincipal User user) {

        RevisionPlanDto plan = revisionPlannerService.getTodaysRevisionPlan(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Today's revision plan retrieved successfully", plan));
    }
}
