package com.automationintesting.api;

import com.automationintesting.db.BrandingDB;
import com.automationintesting.model.Branding;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.sql.SQLException;

@RestController
public class BrandingController {

    private BrandingDB brandingDB;

    @Bean
    public WebMvcConfigurer configurer() {

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String originHost = "http://localhost:3003";

                if(System.getenv("cors") != null){
                    originHost = System.getenv("cors");
                }

                registry.addMapping("/*")
                        .allowedMethods("GET", "POST", "DELETE", "PUT")
                        .allowedOrigins(originHost)
                        .allowCredentials(true);
            }
        };
    }

    public BrandingController() throws SQLException {
        this.brandingDB = new BrandingDB();
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<Branding> getBranding() throws SQLException {
        Branding branding = brandingDB.queryBranding();

        return ResponseEntity.ok(branding);
    }

    @RequestMapping(value = "/", method = RequestMethod.PUT)
    public ResponseEntity<?> createBooking(@RequestBody Branding branding) throws SQLException {
        Branding updatedBranding = brandingDB.update(branding);

        return ResponseEntity.ok(updatedBranding);
    }

}
