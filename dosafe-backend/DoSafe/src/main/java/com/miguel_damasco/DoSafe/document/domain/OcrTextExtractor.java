package com.miguel_damasco.DoSafe.document.domain;

import java.util.List;

public interface OcrTextExtractor {
    List<String> extractLines(String pJobId);
}
