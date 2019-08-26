package com.li.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ApplicationContext {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		CityService cityService = (CityService) context.getBean("cityService");
	}
}
