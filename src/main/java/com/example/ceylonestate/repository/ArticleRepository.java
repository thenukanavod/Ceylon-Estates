package com.example.ceylonestate.repository;

import com.example.ceylonestate.model.Article;
import com.example.ceylonestate.model.ArticleTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findAllByOrderByIdAsc();

    List<Article> findByTopicOrderByIdAsc(ArticleTopic topic);
}
