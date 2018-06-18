package tz.co.nezatech.fixengine.server.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {
    @GetMapping
    public String index(Model model) {
        model.addAttribute("isHome", true);
        return "home.html";
    }
    @GetMapping("home")
    public String home() {
        return "redirect:/";
    }
}
