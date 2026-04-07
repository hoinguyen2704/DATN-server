package com.hoz.hozitech.application.services.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FileStorageServiceTest {

    @Test
    void shouldExtractKeyFromVirtualHostedStyleUrl() {
        String url = "https://my-bucket.s3.ap-southeast-1.amazonaws.com/avatars/user-1.png";

        String key = FileStorageService.extractObjectKey(url, "my-bucket");

        assertEquals("avatars/user-1.png", key);
    }

    @Test
    void shouldExtractKeyFromPathStyleUrl() {
        String url = "https://s3.ap-southeast-1.amazonaws.com/my-bucket/banners/home%201.png?X-Amz-Signature=abc";

        String key = FileStorageService.extractObjectKey(url, "my-bucket");

        assertEquals("banners/home 1.png", key);
    }

    @Test
    void shouldExtractKeyFromDirectPathFallback() {
        String key = FileStorageService.extractObjectKey("articles/post-1.jpg", "my-bucket");

        assertEquals("articles/post-1.jpg", key);
    }

    @Test
    void shouldReturnNullForBlankInput() {
        assertNull(FileStorageService.extractObjectKey("   ", "my-bucket"));
    }
}
