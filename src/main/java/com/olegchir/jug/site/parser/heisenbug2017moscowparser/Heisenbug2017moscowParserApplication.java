package com.olegchir.jug.site.parser.heisenbug2017moscowparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class Heisenbug2017moscowParserApplication {

	public static void main(String[] args) {
		SpringApplication.run(Heisenbug2017moscowParserApplication.class, args);
	}

	@Bean
	public AppRunner appRunner() {
		return new AppRunner();
	}
}
