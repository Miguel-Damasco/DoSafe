package com.miguel_damasco.DoSafe.document.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.miguel_damasco.DoSafe.document.domain.DocumentId;
import com.miguel_damasco.DoSafe.document.domain.DocumentModel;
import com.miguel_damasco.DoSafe.document.domain.DocumentStatus;
import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;
import com.miguel_damasco.DoSafe.document.domain.clasification.GeneralDocumentClasifier;
import com.miguel_damasco.DoSafe.document.infraestructure.ocr.textract.TextractClientAdapter;
import com.miguel_damasco.DoSafe.document.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProcessTextractResultService {
    
    private final DocumentRepository documentRepository;

    private final TextractClientAdapter textractClientAdapter;

    private final GeneralDocumentClasifier generalDocumentClasifier;

    private final ExpirationDateExtractorSelector expirationDateExtractorSelector;

    public void execute(DocumentId pId, String pJobId) {

        log.info("Processing Textract result documentId={} jobId={}", pId.value(), pJobId);

        DocumentModel document = this.documentRepository.findById(pId.value())
                .orElseThrow(() -> new IllegalStateException("Document not found documentId=" + pId.value()));

        if(document.getStatus() == DocumentStatus.PROCESSED) {
            log.info("Document already processed, skipping documentId={}", pId.value());
            return;
        }

        try {

            List<String> result = this.textractClientAdapter.getLines(pJobId);

            log.info("OCR lines retrieved documentId={} lineCount={}", pId.value(), result.size());

            DocumentTypeEnum documentType = this.generalDocumentClasifier.classify(result);

            log.info("Document classified documentId={} type={}", pId.value(), documentType);

            document.setDocumentType(documentType);

            var expirationExtractor = this.expirationDateExtractorSelector.selectExtractor(document);

            LocalDate extractedDate = expirationExtractor.extract(result, document.getId(), document.getUser().getId());

            log.info("Expiration date extracted documentId={} date={}", pId.value(), extractedDate);

            document.setExpireAt(extractedDate);

            document.markProcessed();

            this.documentRepository.save(document);

            log.info("Document updated successfully documentId={}", pId.value());

        } catch (Exception e) {
            log.error("Failed to process document, marking as FAILED documentId={}", pId.value(), e);
            document.markFailed();
            this.documentRepository.save(document);
        }
    }
}
