package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalFileStorageService {

    private final Path root;

    public LocalFileStorageService(@Value("${app.storage.root:./storage}") String rootPath) {
        this.root = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    public String save(byte[] bytes, String relativePath) {
        try {
            Path target = root.resolve(relativePath).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalArgumentException("Nedozvoljena putanja fajla.");
            }
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            return relativePath.replace('\\', '/');
        } catch (IOException ex) {
            throw new IllegalStateException("Greška pri čuvanju fajla: " + ex.getMessage(), ex);
        }
    }

    public Resource loadAsResource(String relativePath) {
        try {
            Path file = root.resolve(relativePath).normalize();
            if (!file.startsWith(root) || !Files.exists(file)) {
                throw new NotFoundException("Fajl izveštaja nije pronađen.");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("Fajl izveštaja nije dostupan.");
            }
            return resource;
        } catch (NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new NotFoundException("Fajl izveštaja nije pronađen.");
        }
    }
}
