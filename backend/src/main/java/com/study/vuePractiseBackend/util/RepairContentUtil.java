package com.study.vuePractiseBackend.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import java.nio.charset.StandardCharsets;

public final class RepairContentUtil {

    private RepairContentUtil() {
    }

    public static String cleanRequiredHtml(
            String content, String fieldName) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }

        if (content.length() > 20000) {
            throw new IllegalArgumentException(
                    fieldName + "不能超过20000个字符");
        }

        Safelist safelist = new Safelist().addTags(
                "p", "br", "b", "strong", "i", "em",
                "u", "ul", "ol", "li", "blockquote");

        String cleaned = Jsoup.clean(
                content,
                "",
                safelist,
                new Document.OutputSettings().prettyPrint(false));

        String text = Jsoup.parseBodyFragment(cleaned)
                .text()
                .replace('\u00A0', ' ')
                .replace("\u200B", "")
                .replace("\uFEFF", "");

        if (text.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "必须包含有效文字");
        }

        // 保持在MySQL TEXT容量以内。
        if (cleaned.getBytes(StandardCharsets.UTF_8).length > 60000) {
            throw new IllegalArgumentException(fieldName + "内容过长");
        }

        return cleaned;
    }
}