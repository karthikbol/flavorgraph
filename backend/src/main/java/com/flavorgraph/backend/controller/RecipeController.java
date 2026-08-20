package com.flavorgraph.backend.controller;

import com.flavorgraph.backend.dto.ApiDtos.*;
import com.flavorgraph.backend.service.FlavorGraphService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/recipes")
public class RecipeController {
    private final FlavorGraphService service;
    public RecipeController(FlavorGraphService service) { this.service=service; }
    @PostMapping("/search") List<RecipeSummary> search(@Valid @RequestBody RecipeSearchRequest request) { return service.search(request); }
    @GetMapping("/{id}") RecipeDetail detail(@PathVariable String id, @RequestParam(defaultValue="") List<String> pantry) {
        return service.detail(id, pantry.stream().filter(s->!s.isBlank()).toList());
    }
    @PostMapping("/{id}/availability") RecipeDetail availability(@PathVariable String id, @Valid @RequestBody AvailabilityRequest request) {
        return service.detail(id, request.ingredientIds());
    }
}
