package com.miguel_damasco.DoSafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DoSafeApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(DoSafeApplication.class, args);
	}

}
