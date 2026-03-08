package com.miguel_damasco.DoSafe.document.domain.extraction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IdentityCardDateExtractor implements ExpirationDateExtractor {

    @Override
    public LocalDate extract(List<String> pLines, UUID pDocumentId, long pUserId) {
       
        LocalDate result = null;

        for(int i = 0; i < pLines.size(); i++) {

            log.trace("Scanning line='{}' documentId={} userId={}", pLines.get(i), pDocumentId, pUserId);

            if(i < pLines.size() - 1 && pLines.get(i).equals("Vencimiento / Vencimento")) {

                log.trace("Expiration key detected line={} documentId={} userId={}", i, pDocumentId, pUserId);
        		
        		DateTimeFormatter formateador = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        		
        		result = LocalDate.parse(pLines.get(i + 1), formateador);

                log.info("Expiration date extracted documentId={} userId={} date={}", pDocumentId, pUserId, result);
        		break;
        	}
        }

        if(result == null) log.warn("Expiration date not found documentId={} userId={}", pDocumentId, pUserId);

        return result;
    }

    @Override
    public boolean supports(DocumentTypeEnum type) {
        return type == DocumentTypeEnum.IDENTITY_CARD;
    }
    
}
