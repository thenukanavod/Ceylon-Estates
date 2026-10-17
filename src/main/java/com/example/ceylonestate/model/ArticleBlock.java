package com.example.ceylonestate.model;

import java.util.List;

/**
 * One piece of an article body, ready for the template to draw. Only the fields that
 * matter for its type are filled in; everything is plain text (the template escapes it).
 */
public record ArticleBlock(Type type, String text, String label, List<String> items, List<List<String>> rows) {

    public enum Type { HEADING, SUBHEADING, PARAGRAPH, LABELLED, LIST, TABLE, IMAGE }

    public boolean isHeading()    { return type == Type.HEADING; }
    public boolean isSubheading() { return type == Type.SUBHEADING; }
    public boolean isParagraph()  { return type == Type.PARAGRAPH; }
    public boolean isLabelled()   { return type == Type.LABELLED; }
    public boolean isList()       { return type == Type.LIST; }
    public boolean isTable()      { return type == Type.TABLE; }
    public boolean isImage()      { return type == Type.IMAGE; }
}
