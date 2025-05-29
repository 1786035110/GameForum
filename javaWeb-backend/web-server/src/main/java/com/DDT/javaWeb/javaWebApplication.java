package com.DDT.javaWeb;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Slf4j
@MapperScan("com.DDT.javaWeb.mapper")
public class javaWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(javaWebApplication.class, args);
        log.info("server started");
    }
}
