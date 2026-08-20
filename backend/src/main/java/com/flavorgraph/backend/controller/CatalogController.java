package com.flavorgraph.backend.controller;

import com.flavorgraph.backend.dto.ApiDtos.Option;
import com.flavorgraph.backend.service.FlavorGraphService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api")
public class CatalogController {
    private final FlavorGraphService service;
    public CatalogController(FlavorGraphService service) { this.service=service; }
    @GetMapping("/ingredients") List<Option> ingredients() { return service.ingredients(); }
    @GetMapping("/cuisines") List<Option> cuisines() { return service.cuisines(); }
    @GetMapping("/diets") List<Option> diets() { return service.diets(); }
    @GetMapping("/appliances") List<Option> appliances() { return service.appliances(); }
}
