package com.dali.fupicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.dali.fupicturebackend.mapper")
public class FuPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FuPictureBackendApplication.class, args);
    }

}
