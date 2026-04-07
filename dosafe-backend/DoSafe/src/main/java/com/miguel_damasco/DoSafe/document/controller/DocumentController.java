package com.miguel_damasco.DoSafe.document.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponse;
import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponses;
import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;
import com.miguel_damasco.DoSafe.document.dto.request.UpdateExpirationDateRequestDTO;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentPageResponseDTO;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentSummaryResponseDTO;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentUploadResponseDTO;
import com.miguel_damasco.DoSafe.document.service.DocumentQueryService;
import com.miguel_damasco.DoSafe.document.service.DocumentUpdateService;
import com.miguel_damasco.DoSafe.document.service.DocumentUploadService;
import com.miguel_damasco.DoSafe.security.MyUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/document")
// @Tag — groups this endpoint under "Documents" in the Swagger UI sidebar.
@Tag(name = "Documents", description = "Identity document upload and processing")
public class DocumentController {

    private final DocumentUploadService documentUploadService;
    private final DocumentQueryService documentQueryService;
    private final DocumentUpdateService documentUpdateService;

    @Operation(
        summary = "Upload document",
        description = "Uploads an identity document (JPG, PNG, PDF). The file is converted to PDF, stored in S3, and sent to AWS Textract for OCR processing. The expiration date is extracted asynchronously."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Document uploaded — OCR processing started"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "File could not be processed")
    })
    // @SecurityRequirement — tells Swagger this endpoint requires a JWT token.
    // Shows a lock icon in the UI and includes the Authorization header when using "Try it out".
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentUploadResponseDTO>> upload(
            @RequestParam("file") MultipartFile pFile,
            @AuthenticationPrincipal MyUserDetails pUserDetails) throws IOException {

        log.info("Document upload request username={} filename={} size={}",
                pUserDetails.getUsername(),
                pFile.getOriginalFilename(),
                pFile.getSize());

        DocumentUploadResponseDTO response = documentUploadService.upload(
                pUserDetails.getUsername(),
                pFile.getInputStream(),
                pFile.getSize(),
                pFile.getOriginalFilename());

        log.info("Document uploaded successfully username={} documentId={}", pUserDetails.getUsername(), response.id());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(response, 201, "Resource created successfully!"));
    }

    @Operation(
        summary = "List my documents",
        description = "Returns a paginated, newest-first list of documents uploaded by the authenticated user. " +
                      "Use 'page' (0-based) and 'size' (max 50) to navigate pages."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of documents returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my-documents")
    public ResponseEntity<ApiResponse<DocumentPageResponseDTO>> listMyDocuments(
            @AuthenticationPrincipal MyUserDetails pUserDetails,
            @RequestParam(defaultValue = "0") int pPage,
            @RequestParam(defaultValue = "10") int pSize) {

        log.info("List documents request username={} page={} size={}", pUserDetails.getUsername(), pPage, pSize);

        DocumentPageResponseDTO response = documentQueryService.listByUser(pUserDetails.getUsername(), pPage, pSize);

        return ResponseEntity.ok(ApiResponses.success(response, 200, "Documents retrieved successfully!"));
    }

    @Operation(
        summary = "List my documents by type",
        description = "Returns a paginated, newest-first list of documents uploaded by the authenticated user " +
                      "filtered by document type. Valid values: IDENTITY_CARD, PASSPORT, DRIVER_LICENCE, OTHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of documents returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid document type value"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my-documents/by-type")
    public ResponseEntity<ApiResponse<DocumentPageResponseDTO>> listMyDocumentsByType(
            @AuthenticationPrincipal MyUserDetails pUserDetails,
            @RequestParam("type") DocumentTypeEnum pType,
            @RequestParam(defaultValue = "0") int pPage,
            @RequestParam(defaultValue = "10") int pSize) {

        log.info("List documents by type request username={} type={} page={} size={}", pUserDetails.getUsername(), pType, pPage, pSize);

        DocumentPageResponseDTO response = documentQueryService.listByUserAndType(pUserDetails.getUsername(), pType, pPage, pSize);

        return ResponseEntity.ok(ApiResponses.success(response, 200, "Documents retrieved successfully!"));
    }

    @Operation(
        summary = "List my expired documents",
        description = "Returns a paginated list of expired documents uploaded by the authenticated user, " +
                      "ordered by expiration date ascending (oldest-expired first). " +
                      "Only fully processed documents are included."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of expired documents returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my-documents/expired")
    public ResponseEntity<ApiResponse<DocumentPageResponseDTO>> listMyExpiredDocuments(
            @AuthenticationPrincipal MyUserDetails pUserDetails,
            @RequestParam(defaultValue = "0") int pPage,
            @RequestParam(defaultValue = "10") int pSize) {

        log.info("List expired documents request username={} page={} size={}", pUserDetails.getUsername(), pPage, pSize);

        DocumentPageResponseDTO response = documentQueryService.listExpiredByUser(pUserDetails.getUsername(), pPage, pSize);

        return ResponseEntity.ok(ApiResponses.success(response, 200, "Expired documents retrieved successfully!"));
    }

    @Operation(
        summary = "Update expiration date",
        description = "Allows the authenticated user to manually set or correct the expiration date of one of their documents."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expiration date updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Document belongs to another user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Document not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/expiration-date")
    public ResponseEntity<ApiResponse<DocumentSummaryResponseDTO>> updateExpirationDate(
            @PathVariable("id") String pId,
            @RequestBody UpdateExpirationDateRequestDTO pRequest,
            @AuthenticationPrincipal MyUserDetails pUserDetails) {

        log.info("Update expiration date request documentId={} username={}", pId, pUserDetails.getUsername());

        DocumentSummaryResponseDTO response = documentUpdateService.updateExpirationDate(
                pUserDetails.getUsername(),
                pId,
                pRequest.expireAt());

        return ResponseEntity.ok(ApiResponses.success(response, 200, "Expiration date updated successfully!"));
    }
}
