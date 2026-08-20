package com.flavorgraph.backend.service;

import com.flavorgraph.backend.dto.ApiDtos.*;
import com.flavorgraph.backend.repository.FlavorGraphRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlavorGraphService {
    private final FlavorGraphRepository repository;
    public FlavorGraphService(FlavorGraphRepository repository) { this.repository=repository; }
    public List<Option> ingredients() { return repository.options("Ingredient"); }
    public List<Option> cuisines() { return repository.options("Cuisine"); }
    public List<Option> diets() { return repository.options("Diet"); }
    public List<Option> appliances() { return repository.options("Appliance"); }
    public List<RecipeSummary> search(RecipeSearchRequest request) { return repository.search(request); }
    public RecipeDetail detail(String id, List<String> pantry) { return repository.detail(id, pantry); }
    public GraphResponse graph() { return repository.graph(); }
}
