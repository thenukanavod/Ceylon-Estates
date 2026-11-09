package com.example.ceylonestate.config;

import com.example.ceylonestate.model.StoredImage;
import com.example.ceylonestate.service.StoredImageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Serves /uploads/** straight from the stored_images table.
 *
 * This is deliberately a plain request handler (registered in UploadsConfig)
 * rather than a @Controller: the site's @ControllerAdvice classes run several
 * database queries before every controller method, which would be wasteful for
 * every single photo on a page - especially with a database in the cloud.
 */
@Component
public class UploadRequestHandler implements HttpRequestHandler {

    private static final String PREFIX = "/uploads/";
    private static final Pattern KEY = Pattern.compile("(properties|brokers|avatars)/[A-Za-z0-9._-]{1,100}");

    private final StoredImageService images;

    public UploadRequestHandler(StoredImageService images) {
        this.images = images;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String method = request.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith(PREFIX)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String key = path.substring(PREFIX.length());
        if (key.contains("..") || !KEY.matcher(key).matches()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Optional<StoredImage> image = images.find(key);
        if (image.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        byte[] data = image.get().getData();
        response.setContentType(image.get().getContentType());
        response.setContentLength(data.length);
        // File names are random UUIDs and never change, so browsers may cache them for a month.
        response.setHeader("Cache-Control", "public, max-age=2592000");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if ("GET".equals(method)) {
            response.getOutputStream().write(data);
        }
    }
}
