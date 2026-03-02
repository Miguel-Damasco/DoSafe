package com.miguel_damasco.DoSafe.document.domain.clasification;

import java.util.List;

import org.springframework.stereotype.Component;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

@Component
public class GeneralDocumentClasifier implements DocumentClasifier {

    @Override
    public DocumentTypeEnum classify(List<String> pLines) {
        
        DocumentTypeEnum result = null;

        for(String line : pLines) {

            result = switch(line) {

                case "Documento de Identidad" -> DocumentTypeEnum.IDENTITY_CARD;
                default -> DocumentTypeEnum.OTHER;
            };

            if(result != DocumentTypeEnum.OTHER) break;
        }

        return result;
    }
    
}
