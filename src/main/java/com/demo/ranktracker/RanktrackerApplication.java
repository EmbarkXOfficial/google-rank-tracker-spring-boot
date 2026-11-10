package com.demo.ranktracker;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RanktrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RanktrackerApplication.class, args);
	}

//	@Bean
//	CommandLineRunner tryIt(RankTracker rankTracker) {
//		return args -> rankTracker.check("spring boot tutorial embarkx");
//	}

}
