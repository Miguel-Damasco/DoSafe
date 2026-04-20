package com.miguel_damasco.DoSafe.alert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.miguel_damasco.DoSafe.alert.dto.AlertCountResponseDTO;
import com.miguel_damasco.DoSafe.alert.dto.AlertPageResponseDTO;
import com.miguel_damasco.DoSafe.alert.service.AlertQueryService;
import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponse;
import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponses;
import com.miguel_damasco.DoSafe.security.MyUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/alert")
@Tag(name = "Alerts", description = "User alert notifications")
public class AlertController {

    private final AlertQueryService alertQueryService;

    @Operation(summary = "Count unread alerts",
               description = "Returns the number of sent alerts not yet read by the authenticated user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unread count returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my-alerts/unread-count")
    public ResponseEntity<ApiResponse<AlertCountResponseDTO>> getUnreadCount(
            @AuthenticationPrincipal MyUserDetails pUserDetails) {

        log.info("Unread alert count request username={}", pUserDetails.getUsername());

        long count = alertQueryService.countUnread(pUserDetails.getUsername());

        return ResponseEntity.ok(ApiResponses.success(
                new AlertCountResponseDTO(count),
                200,
                "Unread alert count retrieved successfully!"));
    }

    @Operation(summary = "List my alerts",
               description = "Returns a paginated list of sent alerts for the authenticated user, newest first.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of alerts returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my-alerts")
    public ResponseEntity<ApiResponse<AlertPageResponseDTO>> listMyAlerts(
            @AuthenticationPrincipal MyUserDetails pUserDetails,
            @RequestParam(defaultValue = "0") int pPage,
            @RequestParam(defaultValue = "20") int pSize) {

        log.info("List alerts request username={} page={} size={}", pUserDetails.getUsername(), pPage, pSize);

        AlertPageResponseDTO response = alertQueryService.listSentAlerts(
                pUserDetails.getUsername(), pPage, pSize);

        return ResponseEntity.ok(ApiResponses.success(response, 200, "Alerts retrieved successfully!"));
    }

    @Operation(summary = "Mark all alerts as read",
               description = "Sets readAt on every unread sent alert belonging to the authenticated user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alerts marked as read"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/my-alerts/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal MyUserDetails pUserDetails) {

        log.info("Mark all alerts read request username={}", pUserDetails.getUsername());

        alertQueryService.markAllRead(pUserDetails.getUsername());

        return ResponseEntity.ok(ApiResponses.success(null, 200, "Alerts marked as read successfully!"));
    }
}
