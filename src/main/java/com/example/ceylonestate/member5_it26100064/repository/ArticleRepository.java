package com.example.ceylonestate.member5_it26100064.repository;

import com.example.ceylonestate.member5_it26100064.model.Article;
import com.example.ceylonestate.member5_it26100064.model.ArticleTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findAllByOrderByIdAsc();

    List<Article> findByTopicOrderByIdAsc(ArticleTopic topic);
}
