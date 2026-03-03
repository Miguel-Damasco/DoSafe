package com.miguel_damasco.DoSafe.document.domain.extraction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;
import org.w3c.dom.DocumentType;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

@Component
public class IdentityCardDateExtractor implements ExpirationDateExtractor {

    @Override
    public LocalDate extract(List<String> pLines) {
       
        LocalDate result = null;

        for(int i = 0; i < pLines.size(); i++) {

            if(i < pLines.size() && pLines.get(i).equals("Vencimiento / Vencimento")) {
        		
        		DateTimeFormatter formateador = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        		
        		result = LocalDate.parse(pLines.get(i + 1), formateador);
        		break;
        	}
        }

        return result;
    }

    @Override
    public boolean supports(DocumentTypeEnum type) {
        return type == DocumentTypeEnum.IDENTITY_CARD;
    }
    
}
