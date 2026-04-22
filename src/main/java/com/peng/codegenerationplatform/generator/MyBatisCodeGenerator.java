package com.peng.codegenerationplatform.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.peng.codegenerationplatform.config.EnvConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

public class MyBatisCodeGenerator {

    // 需要生成的表名
    private static final String[] TABLE_NAMES = {"chat_history"};

    public static void main(String[] args) {
        // 从环境变量获取数据源信息
        // 在 main 方法中使用
        String url = EnvConfig.get("DB_URL");
        String username = EnvConfig.get("DB_USERNAME");
        String password = EnvConfig.get("DB_PASSWORD");

        System.out.println("=== 环境变量检查 ===");
        System.out.println("DB_URL: " + url);
        System.out.println("DB_USERNAME: " + username);
        System.out.println("DB_PASSWORD: " + (password != null ? "***" : null));
        System.out.println("==================");

        if (url == null || username == null || password == null) {
            throw new IllegalStateException(
                    "数据库配置缺失！请设置环境变量：DB_URL, DB_USERNAME, DB_PASSWORD\n"
            );
        }

        // 配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);


        // 创建配置内容
        GlobalConfig globalConfig = createGlobalConfig();

        // 通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        // 生成代码
        generator.generate();
    }

    // 详细配置见：https://mybatis-flex.com/zh/others/codegen.html
    public static GlobalConfig createGlobalConfig() {
        // 创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        // 设置根包，建议先生成到一个临时目录下，生成代码后，再移动到项目目录下
        globalConfig.getPackageConfig()
                .setBasePackage("com.peng.codegenerationplatform.generator");

        // 设置表前缀和只生成哪些表，setGenerateTable 未配置时，生成所有表
        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAMES)
                // 设置逻辑删除的默认字段名称
                .setLogicDeleteColumn("isDelete");

        // 设置生成 entity 并启用 Lombok
        globalConfig.enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21);

        // 设置生成 mapper
        globalConfig.enableMapper();
        globalConfig.enableMapperXml();

        // 设置生成 service
        globalConfig.enableService();
        globalConfig.enableServiceImpl();

        // 设置生成 controller
        globalConfig.enableController();

        // 设置生成时间和字符串为空，避免多余的代码改动
        globalConfig.getJavadocConfig()
                .setAuthor("peng")
                .setSince("");
        return globalConfig;
    }
}

