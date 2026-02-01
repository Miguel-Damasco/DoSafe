package com.miguel_damasco.DoSafe.document.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record DocumentUploadResponseDTO(String id, 
                                            String type, 
                                            String originalFilename, 
                                            Instant createdAt, 
                                            LocalDate expireAt) {}
