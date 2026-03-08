package com.miguel_damasco.DoSafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class DoSafeApplication {
	
	public static void main(String[] args) {

		log.info("HOLA MUNDO!");
		SpringApplication.run(DoSafeApplication.class, args);
	}

}
