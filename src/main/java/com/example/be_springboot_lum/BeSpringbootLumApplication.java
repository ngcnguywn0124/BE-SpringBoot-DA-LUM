package com.example.be_springboot_lum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BeSpringbootLumApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeSpringbootLumApplication.class, args);
	}

}
