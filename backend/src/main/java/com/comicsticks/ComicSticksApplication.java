package com.comicsticks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ComicSticksApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComicSticksApplication.class, args);
    }
}
