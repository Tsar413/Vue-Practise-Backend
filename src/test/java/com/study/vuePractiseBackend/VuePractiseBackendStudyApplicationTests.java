package com.study.vuePractiseBackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;
import java.util.Arrays;

@SpringBootTest(classes = StudyVuePractiseBackendApplication.class)
class VuePractiseBackendStudyApplicationTests {

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void diagnoseMybatisPlus() {

        System.out.println("========== MP诊断开始 ==========");

        // 确认测试容器中是否存在数据源。
        System.out.println(
                "数据源Bean：" +
                        Arrays.toString(context.getBeanNamesForType(DataSource.class))
        );

        // 确认IDEA实际运行时能否加载MP自动配置类，
        // 并打印它来自哪个JAR。
        String className =
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration";

        try {
            Class<?> type = Class.forName(
                    className,
                    false,
                    context.getClassLoader()
            );

            System.out.println("MP自动配置类：存在");

            var source = type.getProtectionDomain().getCodeSource();
            System.out.println(
                    "加载位置：" +
                            (source == null ? "未知" : source.getLocation())
            );
        } catch (ClassNotFoundException | LinkageError e) {
            System.out.println("MP自动配置类加载失败：" + e);
        }

        // 只打印MP相关的自动配置条件，不输出整份报告。
        ConditionEvaluationReport report =
                ConditionEvaluationReport.get(context.getBeanFactory());

        System.out.println("自动配置排除项：" + report.getExclusions());

        boolean found = false;

        for (var entry : report.getConditionAndOutcomesBySource().entrySet()) {
            if (entry.getKey().contains("MybatisPlus")) {
                found = true;
                System.out.println("配置项：" + entry.getKey());

                for (var item : entry.getValue()) {
                    System.out.println(
                            "  条件满足：" + item.getOutcome().isMatch()
                                    + "；原因：" + item.getOutcome().getMessage()
                    );
                }
            }
        }

        if (!found) {
            System.out.println("条件报告中没有任何MybatisPlus配置项");
        }

        System.out.println("========== MP诊断结束 ==========");
    }
}