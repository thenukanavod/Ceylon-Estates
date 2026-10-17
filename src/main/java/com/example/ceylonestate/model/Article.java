package com.example.ceylonestate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * An article that is read on our own site. The text is stored in a small plain-text
 * format (see ArticleBodyParser) so admins can write it without HTML.
 */
@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String title;

    /** Credit line, e.g. "Daily FT · By Roshan Madawela". Optional, plain text (no link). */
    @Size(max = 120)
    @Column(length = 120)
    private String source;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String summary;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArticleTopic topic;

    /** Full text in the simple article format. Null only for old rows from the link-only version. */
    @NotBlank
    @Lob
    private String body;

    public Article() {
        // needed by JPA and the admin form
    }

    public Article(String title, String source, String summary, ArticleTopic topic, String body) {
        setTitle(title);
        setSource(source);
        setSummary(summary);
        this.topic = topic;
        this.body = body;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = trim(title); }
    public String getSource() { return source; }
    public void setSource(String source) {
        String t = trim(source);
        this.source = (t == null || t.isEmpty()) ? null : t;
    }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = trim(summary); }
    public ArticleTopic getTopic() { return topic; }
    public void setTopic(ArticleTopic topic) { this.topic = topic; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
