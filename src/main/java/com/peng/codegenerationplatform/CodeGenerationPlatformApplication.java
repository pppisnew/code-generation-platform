package com.peng.codegenerationplatform;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
@MapperScan("com.peng.codegenerationplatform.mapper")
public class CodeGenerationPlatformApplication {

    public static void main(String[] args) {
        // 加载.env文件
        Dotenv.configure().ignoreIfMissing().systemProperties().load();
        SpringApplication.run(CodeGenerationPlatformApplication.class, args);
    }

}
