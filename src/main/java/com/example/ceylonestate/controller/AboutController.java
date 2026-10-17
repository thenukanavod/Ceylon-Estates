package com.example.ceylonestate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** The About Us page. The "Explore more" button in the homepage "Why us" panel links here. */
@Controller
public class AboutController {

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
