package com.hoz.hozitech.application.services.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Stores a product image and returns the URL.
     *
     * @param file the image file
     * @return the URL of the stored file
     */
    String storeProductImage(MultipartFile file);

    /**
     * Generic file upload to a specified folder.
     *
     * @param file the file to upload
     * @param folder the destination folder
     * @return the URL of the uploaded file
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * Deletes a file by its URL.
     *
     * @param fileUrl the URL of the file to delete
     */
    void deleteFile(String fileUrl);

}
