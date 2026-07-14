package com.shopverse;

import org.springframework.boot.SpringApplication;

public class TestShopVerseApplication {

	public static void main(String[] args) {
		SpringApplication.from(ShopVerseApplication::main)
				.with(TestcontainersConfiguration.class)
				.run(args);
	}
}