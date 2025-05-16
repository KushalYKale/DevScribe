package com.DevScribe.editor.highlighting;

import org.fxmisc.richtext.model.StyleSpans;
import java.util.Collection;

public interface LanguageHighlighter {
    StyleSpans<Collection<String>> computeHighlighting(String text);
}