package sn.sunuagri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import sn.sunuagri.application.ConfigurationCooperative;

@SpringBootApplication
@EnableConfigurationProperties(ConfigurationCooperative.class)
public class SunuAgriApplication {

    public static void main(String[] args) {
        SpringApplication.run(SunuAgriApplication.class, args);
    }
}
