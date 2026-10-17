package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.Article;
import com.example.ceylonestate.model.ArticleTopic;
import com.example.ceylonestate.repository.ArticleRepository;
import com.example.ceylonestate.service.ArticleBodyParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Public Articles pages: the list (/articles) and one article to read on our site (/articles/{id}). */
@Controller
public class ArticleController {

    private final ArticleRepository articles;

    public ArticleController(ArticleRepository articles) {
        this.articles = articles;
    }

    @GetMapping("/articles")
    public String list(@RequestParam(required = false) String topic, Model model) {
        ArticleTopic selected = parse(topic);
        model.addAttribute("articles", selected == null
                ? articles.findAllByOrderByIdAsc()
                : articles.findByTopicOrderByIdAsc(selected));
        model.addAttribute("topics", ArticleTopic.values());
        model.addAttribute("selectedTopic", selected);
        return "articles";
    }

    @GetMapping("/articles/{id}")
    public String read(@PathVariable Long id, Model model) {
        Article article = articles.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("article", article);
        model.addAttribute("blocks", ArticleBodyParser.parse(article.getBody()));
        model.addAttribute("related", articles.findByTopicOrderByIdAsc(article.getTopic()).stream()
                .filter(a -> !a.getId().equals(id)).limit(3).toList());
        return "article";
    }

    private static ArticleTopic parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ArticleTopic.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
