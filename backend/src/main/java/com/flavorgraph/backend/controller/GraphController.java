package com.flavorgraph.backend.controller;

import com.flavorgraph.backend.dto.ApiDtos.GraphResponse;
import com.flavorgraph.backend.service.FlavorGraphService;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/graph")
public class GraphController {
    private final FlavorGraphService service;
    public GraphController(FlavorGraphService service) { this.service=service; }
    @GetMapping GraphResponse graph() { return service.graph(); }
}
