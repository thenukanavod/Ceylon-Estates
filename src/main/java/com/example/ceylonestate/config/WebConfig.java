package com.example.ceylonestate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.nio.file.Paths;
import java.util.Locale;

/**
 * Uploaded avatar images are saved OUTSIDE the compiled project (in an
 * "uploads" folder next to where the app runs), not inside src/main/resources.
 * This tells Spring "when someone requests /uploads/xyz.jpg, fetch it from
 * that external folder" - otherwise the browser would get a 404.
 *
 * Also configures language switching (English / Sinhala) - the chosen
 * language is remembered per browser session via ?lang=en or ?lang=si.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Spring only treats a resource location as a folder if it ends with "/".
        // Path.toUri() adds that slash only when the folder already exists, so
        // on a fresh start (no uploads folder yet) every uploaded image 404'd
        // until the app was restarted. Create the folder first and force the slash.
        java.nio.file.Path uploadsDir = Paths.get("uploads").toAbsolutePath();
        try {
            java.nio.file.Files.createDirectories(uploadsDir);
        } catch (java.io.IOException ignored) {
            // the storage services create it again on first upload
        }
        String uploadsPath = uploadsDir.toUri().toString();
        if (!uploadsPath.endsWith("/")) {
            uploadsPath += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsPath);
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang"); // switch language by visiting any page with ?lang=si
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
