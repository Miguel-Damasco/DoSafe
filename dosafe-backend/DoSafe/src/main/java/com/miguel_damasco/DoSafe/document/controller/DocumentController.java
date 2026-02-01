package com.miguel_damasco.DoSafe.document.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping(path = "/document")
public class DocumentController {
    
    private final DocumentUploadService documentUploadService;

    public DocumentController(DocumentUploadService pDocumentUploadService) {
        this.documentUploadService = pDocumentUploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentUploadResponseDTO>> upload(
                                                    @RequestParam("file") MultipartFile pFile, 
                                                    @AuthenticationPrincipal MyUserDetails pUserDetails) throws IOException {


    System.out.println("LLega!");

    DocumentUploadResponseDTO response = this.documentUploadService.upload(pUserDetails.getUsername(),pFile.getInputStream(),pFile.getSize(), pFile.getOriginalFilename());

    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponses.success(response, 201, "Resource created succesfully!"));

    }
}
