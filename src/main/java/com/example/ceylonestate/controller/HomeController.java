package com.example.ceylonestate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Renders the homepage using Thymeleaf (server-side HTML templates).
 * Visit: http://localhost:8080/
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("appName", "Ceylon Estates");
        return "index"; // resolves to src/main/resources/templates/index.html
    }
}
