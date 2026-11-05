package it.exprivia.fornitori;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class FornitoriApplication {

	public static void main(String[] args) {
		SpringApplication.run(FornitoriApplication.class, args);
	}

}
