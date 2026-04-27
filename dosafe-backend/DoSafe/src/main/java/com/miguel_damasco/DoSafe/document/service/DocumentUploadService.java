package com.miguel_damasco.DoSafe.document.service;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.common.exception.RateLimitExceededException;
import com.miguel_damasco.DoSafe.document.domain.DocumentFactory;
import io.micrometer.core.instrument.MeterRegistry;
import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.dto.mapper.DocumentResponseMapper;
import com.miguel_damasco.DoSafe.document.dto.response.DocumentUploadResponseDTO;
import com.miguel_damasco.DoSafe.document.infraestructure.conversion.ConvertedDocument;
import com.miguel_damasco.DoSafe.document.infraestructure.conversion.DocumentConverter;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;
import com.miguel_damasco.DoSafe.storage.DocumentStorage;
import com.miguel_damasco.DoSafe.user.domain.UserModel;
import com.miguel_damasco.DoSafe.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentUploadService {

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final DocumentConverter documentConverter;
    private final UserService userService;
    private final MeterRegistry meterRegistry;

    @Value("${ratelimit.upload.per-user-daily:3}")
    private int perUserDailyLimit;

    @Value("${ratelimit.upload.global-daily:10}")
    private int globalDailyLimit;

    public DocumentUploadResponseDTO upload(String pUsername, InputStream pContent, long pSize, String pOriginalFilename) {

        UserModel user = this.userService.findUserByUsername(pUsername);

        checkRateLimits(user);

        DocumentModel newDocument = DocumentFactory.create(user, pOriginalFilename, null);

        DocumentModel savedDocuemnt = this.documentRepository.save(newDocument);

        ConvertedDocument pdf = this.documentConverter.convertToPdf(pContent, pOriginalFilename, "multipart/form-data");

        String key = this.documentStorage.upload(user.getId(), savedDocuemnt.getId().toString(), pdf.content(), pdf.size(), pdf.contentType());

        savedDocuemnt.setKey(key);

        this.documentRepository.save(savedDocuemnt);

        meterRegistry.counter("dosafe.documents.uploaded", "result", "success").increment();

        return DocumentResponseMapper.toDto(savedDocuemnt);
    }

    // Enforces two independent limits before any S3/Textract calls are made:
    //   1. Per-user: at most perUserDailyLimit uploads per calendar day (UTC).
    //   2. Global: at most globalDailyLimit uploads across all users per calendar day (UTC).
    // Using UTC midnight as the day boundary keeps the window consistent regardless
    // of the server's local timezone or where users are located.
    private void checkRateLimits(UserModel pUser) {

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        long userCount = documentRepository.countByUserAndCreatedAtBetween(pUser, startOfDay, endOfDay);
        if (userCount >= perUserDailyLimit) {
            log.warn("Per-user upload limit reached userId={} count={} limit={}", pUser.getId(), userCount, perUserDailyLimit);
            throw new RateLimitExceededException(
                String.format("You have reached the daily upload limit of %d documents. Try again tomorrow.", perUserDailyLimit)
            );
        }

        long globalCount = documentRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        if (globalCount >= globalDailyLimit) {
            log.warn("Global upload limit reached count={} limit={}", globalCount, globalDailyLimit);
            throw new RateLimitExceededException(
                "The daily document processing limit has been reached. Please try again tomorrow."
            );
        }
    }
}
