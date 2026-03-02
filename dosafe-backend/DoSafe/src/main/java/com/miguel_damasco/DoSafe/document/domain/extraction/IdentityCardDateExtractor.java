package com.miguel_damasco.DoSafe.document.domain.extraction;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;
import org.w3c.dom.DocumentType;

@Component
public class IdentityCardDateExtractor implements ExpirationDateExtractor {

    @Override
    public LocalDate extract(List<String> pLines) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'extract'");
    }

    @Override
    public boolean supports(DocumentType type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'supports'");
    }
    
}
