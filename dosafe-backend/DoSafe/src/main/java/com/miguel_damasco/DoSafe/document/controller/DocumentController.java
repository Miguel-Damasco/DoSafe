package com.miguel_damasco.DoSafe.document.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponse;
import com.miguel_damasco.DoSafe.common.apiResponse.ApiResponses;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentUploadResponseDTO;
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
}
