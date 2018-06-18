package tz.co.nezatech.fixengine.server.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/settings")
public class SettingsController {
    @GetMapping
    public String index(Model model) {
        model.addAttribute("isSettings", true);
        return "settings.html";
    }
}
