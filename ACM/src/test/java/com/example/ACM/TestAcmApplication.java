package com.example.ACM;

import org.springframework.boot.SpringApplication;

public class TestAcmApplication {

	public static void main(String[] args) {
		SpringApplication.from(AcmApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
