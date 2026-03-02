package com.miguel_damasco.DoSafe.document.infraestructure.ocr.textract;

import java.util.List;

import org.springframework.stereotype.Component;

import com.miguel_damasco.DoSafe.document.domain.OcrTextExtractor;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class TextractTextExtractorAdapter implements OcrTextExtractor {

    private final TextractClientAdapter textractClientAdapter;

    @Override
    public List<String> extractLines(String pJobId) {
        return this.textractClientAdapter.getLines(pJobId);
    }
    
}
