package com.org.javadoc.ai.generator.controller;

import com.org.javadoc.ai.generator.parser.JavaCodeParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class CallGraphController {

    @Autowired
    private JavaCodeParser javaCodeParser;

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("message", "Please select a file to upload.");
            return "index";
        }

        try {
            // Save the file locally
            Path path = Paths.get("uploads/" + file.getOriginalFilename());
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            // Generate call graph
            File javaFile = path.toFile();
            javaCodeParser.parseAndGenerateDocs(javaFile);

            // Read the generated call graph
           // String callGraphPath = "call-graph/" + file.getOriginalFilename().replace(".java", ".txt");
            String callGraphPath = "call-graph/" + "uploadFile.txt";
            String callGraph = new String(Files.readAllBytes(Paths.get(callGraphPath)));

            model.addAttribute("callGraph", callGraph);
            model.addAttribute("message", "File uploaded successfully: " + file.getOriginalFilename());

        } catch (IOException e) {
            model.addAttribute("message", "Failed to upload file: " + e.getMessage());
        }

        return "index";
    }
}