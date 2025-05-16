package com.DevScribe.editor.highlighting;

import java.util.*;
import java.util.regex.*;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public class PythonHighlighter implements LanguageHighlighter {
    private static final String[] KEYWORDS = {
            "False", "await", "else", "import", "pass", "None", "break", "except",
            "in", "raise", "True", "class", "finally", "is", "return", "and", "continue",
            "for", "lambda", "try", "as", "def", "from", "nonlocal", "while", "assert",
            "del", "global", "not", "with", "async", "elif", "if", "or", "yield"
    };

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>\\b(" + String.join("|", KEYWORDS) + ")\\b)"
                    + "|(?<STRING>\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*')"
                    + "|(?<COMMENT>#.*)"
                    + "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)"
    );

    @Override
    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastKwEnd = 0;

        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("STRING") != null ? "string" :
                                    matcher.group("COMMENT") != null ? "comment" :
                                            matcher.group("NUMBER") != null ? "number" :
                                                    null;

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
