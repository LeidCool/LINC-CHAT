package com.leidcool.lincchat.format;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds regex matches in an already-styled {@link Component} (e.g. one produced by a single
 * {@code MiniMessage.deserialize} call, possibly containing a {@code <gradient>}/{@code
 * <rainbow>}) and splices in replacements, while keeping every untouched character's original
 * style intact.
 * <p>
 * This exists instead of the built-in {@code Component#replaceText} because a gradient/rainbow
 * is rendered by MiniMessage as one child {@link TextComponent} <em>per character</em> (each
 * with its own interpolated colour) -- a multi-character token like {@code *item1} would then
 * never be found by the stock API, since it only matches within a single node's own text. Here
 * the whole tree is first flattened into a list of (text, effective style) runs, matched as one
 * concatenated string, then re-split at the match boundaries so a match can freely span several
 * gradient-coloured characters.
 */
final class ComponentTextReplacer {

    private ComponentTextReplacer() {
    }

    /** A single match plus the style that was in effect at its first character. */
    interface MatchHandler {
        /** Return the replacement component, or {@code null} to leave the match's text/style untouched. */
        Component replacement(Matcher match, Style ambientStyle);
    }

    private record Run(String text, Style style) {
    }

    static Component replace(Component input, Pattern pattern, MatchHandler handler) {
        List<Run> runs = new ArrayList<>();
        flatten(input, Style.empty(), runs);
        if (runs.isEmpty()) {
            return input;
        }

        StringBuilder combined = new StringBuilder();
        int[] runStart = new int[runs.size()];
        for (int i = 0; i < runs.size(); i++) {
            runStart[i] = combined.length();
            combined.append(runs.get(i).text());
        }

        Matcher matcher = pattern.matcher(combined);
        if (!matcher.find()) {
            return input;
        }
        matcher.reset();

        List<Component> out = new ArrayList<>();
        int cursor = 0;
        while (matcher.find()) {
            appendRange(runs, runStart, cursor, matcher.start(), out);
            Style ambient = styleAt(runs, runStart, matcher.start());
            Component replacement = handler.replacement(matcher, ambient);
            if (replacement != null) {
                out.add(replacement);
            } else {
                appendRange(runs, runStart, matcher.start(), matcher.end(), out);
            }
            cursor = matcher.end();
        }
        appendRange(runs, runStart, cursor, combined.length(), out);
        return Component.empty().children(out);
    }

    private static void flatten(Component component, Style inherited, List<Run> out) {
        Style effective = inherited.merge(component.style());
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            out.add(new Run(text.content(), effective));
        }
        for (Component child : component.children()) {
            flatten(child, effective, out);
        }
    }

    private static Style styleAt(List<Run> runs, int[] runStart, int offset) {
        for (int i = 0; i < runs.size(); i++) {
            int start = runStart[i];
            int end = start + runs.get(i).text().length();
            if (offset >= start && offset < end) {
                return runs.get(i).style();
            }
        }
        return runs.isEmpty() ? Style.empty() : runs.get(runs.size() - 1).style();
    }

    /** Emits the [start, end) slice of the combined text as one styled component per underlying run. */
    private static void appendRange(List<Run> runs, int[] runStart, int start, int end, List<Component> out) {
        if (start >= end) {
            return;
        }
        for (int i = 0; i < runs.size(); i++) {
            String text = runs.get(i).text();
            int rStart = runStart[i];
            int rEnd = rStart + text.length();
            int segStart = Math.max(start, rStart);
            int segEnd = Math.min(end, rEnd);
            if (segStart < segEnd) {
                out.add(Component.text(text.substring(segStart - rStart, segEnd - rStart), runs.get(i).style()));
            }
        }
    }
}
