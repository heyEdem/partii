package com.theinside.partii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class PartiiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PartiiApplication.class, args);
	}

}
