package dev.flagpole.api;

import org.springframework.boot.SpringApplication;

public class TestFlagpoleApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(FlagpoleApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
