package hu.unideb.inf.szoftverfejlesztes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Az ütemezés az ismétlődő kiadások automatikus létrehozásához kell
@SpringBootApplication
@EnableScheduling
public class SzoftverfejlesztesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SzoftverfejlesztesApplication.class, args);
    }

}
