package com.xxx.redo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class XxxRedoApplication {

    public static void main(String[] args) {
        SpringApplication.run(XxxRedoApplication.class, args);
    }

}
