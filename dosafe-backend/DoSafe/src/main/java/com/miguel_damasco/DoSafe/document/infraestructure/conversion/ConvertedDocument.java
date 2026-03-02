package com.miguel_damasco.DoSafe.document.infraestructure.conversion;

import java.io.InputStream;

public record ConvertedDocument(InputStream content, long size, String contentType) {}
