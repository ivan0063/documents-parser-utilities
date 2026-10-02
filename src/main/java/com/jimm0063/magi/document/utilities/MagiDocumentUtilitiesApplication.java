package com.jimm0063.magi.document.utilities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MagiDocumentUtilitiesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MagiDocumentUtilitiesApplication.class, args);
    }
}
