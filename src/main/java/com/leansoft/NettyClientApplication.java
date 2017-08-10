package com.leansoft;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class NettyClientApplication {
	public static void main(String[] args) {
		SpringApplication.run(NettyClientApplication.class, args);
	}
}
