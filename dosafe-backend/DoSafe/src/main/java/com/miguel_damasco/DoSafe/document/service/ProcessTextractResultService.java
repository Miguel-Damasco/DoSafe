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

@RequiredArgsConstructor
@Service
public class ProcessTextractResultService {
    
    private final DocumentRepository documentRepository;

    private final TextractClientAdapter textractClientAdapter;

    private final GeneralDocumentClasifier generalDocumentClasifier;

    public void execute(DocumentId pId, String pJobId) {

        DocumentModel document = this.documentRepository.findById(pId.value()).orElseThrow();

            if(document.getStatus() == DocumentStatus.PROCESSED) return;

            List<String> result = this.textractClientAdapter.getLines(pJobId);

            DocumentTypeEnum documentType = this.generalDocumentClasifier.classify(result);

            document.setDocumentType(documentType);

            document.setExpireAt(LocalDate.now()); // Just testing

            document.markProcessed();

            this.documentRepository.save(document);

            System.out.println();
            System.out.println("Documento Actualizado con exito!");
            System.out.println("Id: " + document.getId() + " y: " + pId.value());
            System.out.println();

            return;
    }
}
