package com.flavorgraph.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public final class ApiDtos {
    private ApiDtos() {}

    public record Option(String id, String name, String category) {}

    public record RecipeSearchRequest(
            @NotNull List<String> ingredientIds,
            @NotNull List<String> applianceIds,
            String cuisineId,
            @NotNull List<String> dietIds,
            @Min(1) @Max(480) Integer maxCookTimeMinutes) {
        public RecipeSearchRequest {
            ingredientIds = ingredientIds == null ? List.of() : List.copyOf(ingredientIds);
            applianceIds = applianceIds == null ? List.of() : List.copyOf(applianceIds);
            dietIds = dietIds == null ? List.of() : List.copyOf(dietIds);
        }
    }

    public record RecipeSummary(String id, String name, String description, int cookTimeMinutes,
            String difficulty, String cookingMethod, String imageUrl, String cuisine, List<String> diets,
            List<String> appliances, int ingredientMatchPercentage, int matchedIngredientCount,
            int totalIngredientCount, int missingIngredientCount, boolean possibleWithSubstitutions) {}

    public record IngredientLine(String id, String name, String category, double quantity, String unit,
            boolean available, List<Substitution> substitutions) {}

    public record Substitution(String id, String name, double similarityScore, String note, boolean owned) {}

    public record RecipeDetail(String id, String name, String description, int cookTimeMinutes,
            String difficulty, String cookingMethod, String imageUrl, String cuisine, List<String> diets,
            List<String> appliances, List<IngredientLine> ingredients, List<String> instructions) {}

    public record AvailabilityRequest(@NotNull List<String> ingredientIds) {
        public AvailabilityRequest { ingredientIds = ingredientIds == null ? List.of() : List.copyOf(ingredientIds); }
    }

    public record GraphNode(String id, String label, String name, Map<String, Object> properties) {}
    public record GraphEdge(String source, String target, String type, Map<String, Object> properties) {}
    public record GraphResponse(List<GraphNode> nodes, List<GraphEdge> edges) {}
    public record ApiError(String code, String message, Map<String, String> details) {}
}
