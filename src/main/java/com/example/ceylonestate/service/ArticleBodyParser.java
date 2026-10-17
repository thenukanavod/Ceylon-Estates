package com.example.ceylonestate.service;

import com.example.ceylonestate.model.ArticleBlock;
import com.example.ceylonestate.model.ArticleBlock.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the plain-text article format into blocks. The format (blocks are separated
 * by a blank line):
 * <pre>
 * ## Heading
 * ### Smaller heading
 * - bullet            (consecutive lines make one list)
 * | a | b |           (consecutive lines make one table; first row is the header)
 * **Label:** text     (a paragraph that starts with a bold label)
 * !img file.png | alt text   (a picture from /images/articles/)
 * anything else is a normal paragraph
 * </pre>
 * Because the template prints every value as escaped text, nothing typed here can inject HTML.
 */
public final class ArticleBodyParser {

    private static final Pattern LABELLED = Pattern.compile("^\\*\\*(.+?)\\*\\*\\s*(.*)$", Pattern.DOTALL);
    private static final Pattern SAFE_FILE = Pattern.compile("^[A-Za-z0-9._-]+$");

    private ArticleBodyParser() {
    }

    public static List<ArticleBlock> parse(String body) {
        List<ArticleBlock> blocks = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return blocks;
        }
        for (String chunk : body.replace("\r\n", "\n").split("\n\\s*\n")) {
            String text = chunk.strip();
            if (text.isEmpty()) {
                continue;
            }
            String[] lines = text.split("\n");
            if (text.startsWith("### ")) {
                blocks.add(simple(Type.SUBHEADING, text.substring(4)));
            } else if (text.startsWith("## ")) {
                blocks.add(simple(Type.HEADING, text.substring(3)));
            } else if (text.startsWith("!img ")) {
                image(text.substring(5), blocks);
            } else if (text.startsWith("- ")) {
                List<String> items = new ArrayList<>();
                for (String line : lines) {
                    items.add(line.startsWith("- ") ? line.substring(2).strip() : line.strip());
                }
                blocks.add(new ArticleBlock(Type.LIST, null, null, items, null));
            } else if (text.startsWith("|")) {
                blocks.add(table(lines));
            } else {
                Matcher m = LABELLED.matcher(text.replace("\n", " "));
                if (m.matches()) {
                    blocks.add(new ArticleBlock(Type.LABELLED, m.group(2).strip(), m.group(1).strip(), null, null));
                } else {
                    blocks.add(simple(Type.PARAGRAPH, text.replace("\n", " ")));
                }
            }
        }
        return blocks;
    }

    private static ArticleBlock simple(Type type, String text) {
        return new ArticleBlock(type, text.strip(), null, null, null);
    }

    private static void image(String spec, List<ArticleBlock> blocks) {
        String[] parts = spec.split("\\|", 2);
        String file = parts[0].strip();
        if (!SAFE_FILE.matcher(file).matches()) {
            return; // never build a path from anything but a plain file name
        }
        blocks.add(new ArticleBlock(Type.IMAGE, parts.length > 1 ? parts[1].strip() : "", file, null, null));
    }

    private static ArticleBlock table(String[] lines) {
        List<List<String>> rows = new ArrayList<>();
        int width = 0;
        for (String line : lines) {
            String t = line.strip();
            if (t.startsWith("|")) t = t.substring(1);
            if (t.endsWith("|")) t = t.substring(0, t.length() - 1);
            List<String> cells = new ArrayList<>();
            for (String cell : t.split("\\|", -1)) {
                cells.add(cell.strip());
            }
            width = Math.max(width, cells.size());
            rows.add(cells);
        }
        for (List<String> row : rows) {
            while (row.size() < width) {
                row.add("");
            }
        }
        return new ArticleBlock(Type.TABLE, null, null, null, rows);
    }
}
