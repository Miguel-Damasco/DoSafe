package com.miguel_damasco.DoSafe.document.infraestructure.ocr.textract;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.GetDocumentAnalysisRequest;
import software.amazon.awssdk.services.textract.model.GetDocumentAnalysisResponse;

@RequiredArgsConstructor
@Component
public class TextractClientAdapter {
    
    private final TextractClient textractClient;

    public List<String> getLines(String jobId) {

        List<String> lines = new ArrayList<>();
        String nextToken = null;

        do {
            GetDocumentAnalysisResponse response =
                    textractClient.getDocumentAnalysis(
                            GetDocumentAnalysisRequest.builder()
                                    .jobId(jobId)
                                    .nextToken(nextToken)
                                    .build()
                    );

            response.blocks().forEach(block -> {
                if (block.blockType() == BlockType.LINE) {
                    lines.add(block.text());
                }
            });

            nextToken = response.nextToken();

        } while (nextToken != null);

        return lines;
    }
}
