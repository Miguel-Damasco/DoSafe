package com.miguel_damasco.DoSafe.document.domain.clasification;

import java.util.List;

import com.miguel_damasco.DoSafe.document.domain.DocumentTypeEnum;

public interface DocumentClasifier {
    
    DocumentTypeEnum classify(List<String> pLines);
}
