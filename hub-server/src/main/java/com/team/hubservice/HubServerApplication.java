package com.team.hubservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableJpaAuditing
@SpringBootApplication
public class HubServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HubServerApplication.class, args);
	}

}
