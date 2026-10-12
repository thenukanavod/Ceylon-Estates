package com.example.ceylonestate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The admin-only login page. The form posts to /admin/login, which is handled by
 * the admin security chain (see SecurityConfig) - not by this controller.
 */
@Controller
public class AdminAuthController {

    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }
}
