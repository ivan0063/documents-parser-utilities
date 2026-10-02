package com.jimm0063.magi.document.utilities.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.jimm0063.magi.document.utilities.core.ModuleRegistry;

@Controller
public class HomeController {

    private final ModuleRegistry registry;

    public HomeController(ModuleRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("modules", registry.all());
        return "index";
    }
}
