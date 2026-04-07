package com.miguel_damasco.DoSafe.document.domain.clasification;

import java.util.List;

import org.springframework.stereotype.Component;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

@Component
public class GeneralDocumentClasifier implements DocumentClasifier {

    @Override
    public DocumentTypeEnum classify(List<String> pLines) {
        
        DocumentTypeEnum result = DocumentTypeEnum.OTHER;

        for(String line : pLines) {

            DocumentTypeEnum matched = switch(line) {
                case "Documento de Identidad" -> DocumentTypeEnum.IDENTITY_CARD;
                case "PASAPORTE/"             -> DocumentTypeEnum.PASSPORT;
                default                       -> null;
            };

            if(matched != null) {
                result = matched;
                break;
            }
        }

        return result;
    }
    
}
