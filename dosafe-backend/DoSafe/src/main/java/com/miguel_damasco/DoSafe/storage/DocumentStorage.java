package com.miguel_damasco.DoSafe.storage;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

public interface DocumentStorage {
    
    void upload(String pKey, InputStream pContent, long pSize, String pContentType);

    URL generateDownloadUrl(String pKey, Duration pTtl);

    void delete(String pKey);
}
