package com.example.ceylonestate.controller;

import com.example.ceylonestate.model.Article;
import com.example.ceylonestate.model.ArticleTopic;
import com.example.ceylonestate.repository.ArticleRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin CRUD for the Articles page: list, add, edit, delete. */
@Controller
@RequestMapping("/admin/articles")
public class AdminArticleController {

    private final ArticleRepository articles;

    public AdminArticleController(ArticleRepository articles) {
        this.articles = articles;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("articles", articles.findAllByOrderByIdAsc());
        return "admin/articles";
    }

    @GetMapping("/new")
    public String showCreate(Model model) {
        prepare(model, new Article(), false);
        return "admin/article-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("article") Article article, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepare(model, article, false);
            return "admin/article-form";
        }
        articles.save(article);
        redirect.addFlashAttribute("flash", "adminArticles.flash.saved");
        return "redirect:/admin/articles";
    }

    @GetMapping("/{id}/edit")
    public String showEdit(@PathVariable Long id, Model model) {
        prepare(model, find(id), true);
        return "admin/article-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("article") Article form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        Article existing = find(id);
        if (result.hasErrors()) {
            prepare(model, existing, true);
            return "admin/article-form";
        }
        existing.setTitle(form.getTitle());
        existing.setSource(form.getSource());
        existing.setBody(form.getBody());
        existing.setSummary(form.getSummary());
        existing.setTopic(form.getTopic());
        articles.save(existing);
        redirect.addFlashAttribute("flash", "adminArticles.flash.saved");
        return "redirect:/admin/articles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        articles.delete(find(id));
        redirect.addFlashAttribute("flash", "adminArticles.flash.deleted");
        return "redirect:/admin/articles";
    }

    private Article find(Long id) {
        return articles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void prepare(Model model, Article article, boolean editing) {
        model.addAttribute("article", article);
        model.addAttribute("topics", ArticleTopic.values());
        model.addAttribute("editing", editing);
    }
}
