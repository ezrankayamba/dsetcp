package tz.co.nezatech.fixengine.server.ui.controller;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;

@Controller
@RequestMapping("/ocr")
public class OCRController {
    Logger logger = LoggerFactory.getLogger(OCRController.class);
    @Value("${tess.data.path}")
    String dataPath;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("isOcr", true);
        return "ocr.html";
    }

    @PostMapping
    public String readOcr(Model model, @RequestParam("file") MultipartFile file) {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        ITesseract instance = new Tesseract();
        try {
            instance.setDatapath(dataPath);
            String result = instance.doOCR(ImageIO.read(file.getInputStream()));
            model.addAttribute("ocrText", result);
        } catch (TesseractException e) {
            e.printStackTrace();
            model.addAttribute("ocrText", e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("ocrText", e.getMessage());
        }
        logger.debug("Text: " + model);
        model.addAttribute("isOcr", true);
        return "ocr.html";
    }
}

