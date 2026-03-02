package com.miguel_damasco.DoSafe.document.infraestructure.conversion;

import java.io.InputStream;

public interface DocumentConverter {
    
    ConvertedDocument convertToPdf(InputStream input, String originalFilename, String contentType);
}
