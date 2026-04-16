package br.tec.dev2b.whats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WhatsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhatsApplication.class, args);
    }
}
