package com.treasury;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TreasuryApplication {

	public static void main(String[] args) {
		SpringApplication.run(TreasuryApplication.class, args);
	}

}
