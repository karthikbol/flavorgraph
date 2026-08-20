package com.flavorgraph.backend.repository;

import com.flavorgraph.backend.dto.ApiDtos.*;
import com.flavorgraph.backend.exception.ApiExceptions;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class FlavorGraphRepository {
    private final Driver driver;
    public FlavorGraphRepository(Driver driver) { this.driver = driver; }

    public List<Option> options(String label) {
        // Labels cannot be parameters. Select complete static queries so Cypher is never assembled at runtime.
        String cypher = switch (label) {
            case "Ingredient" -> "MATCH (n:Ingredient) RETURN n.id AS id, n.name AS name, coalesce(n.category, '') AS category ORDER BY n.name";
            case "Cuisine" -> "MATCH (n:Cuisine) RETURN n.id AS id, n.name AS name, '' AS category ORDER BY n.name";
            case "Diet" -> "MATCH (n:Diet) RETURN n.id AS id, n.name AS name, '' AS category ORDER BY n.name";
            case "Appliance" -> "MATCH (n:Appliance) RETURN n.id AS id, n.name AS name, '' AS category ORDER BY n.name";
            default -> throw new IllegalArgumentException("Unsupported option label");
        };
        return read(cypher, Map.of(), r -> new Option(r.get("id").asString(), r.get("name").asString(), r.get("category").asString()));
    }

    public List<RecipeSummary> search(RecipeSearchRequest request) {
        String cypher = """
                MATCH (r:Recipe)-[:USES]->(i:Ingredient)
                OPTIONAL MATCH (r)-[:BELONGS_TO]->(c:Cuisine)
                OPTIONAL MATCH (r)-[:SUITABLE_FOR]->(d:Diet)
                OPTIONAL MATCH (r)-[:REQUIRES_APPLIANCE]->(a:Appliance)
                WITH r, c, collect(DISTINCT d) AS dietNodes, collect(DISTINCT a) AS applianceNodes,
                     collect(DISTINCT i) AS ingredients
                WHERE ($cuisineId = '' OR c.id = $cuisineId)
                  AND ($maxTime IS NULL OR r.cookTimeMinutes <= $maxTime)
                  AND (size($applianceIds) = 0 OR any(x IN applianceNodes WHERE x.id IN $applianceIds))
                  AND (size($dietIds) = 0 OR all(wanted IN $dietIds WHERE any(x IN dietNodes WHERE x.id = wanted)))
                WITH r, c, dietNodes, applianceNodes, ingredients,
                     [x IN ingredients WHERE x.id IN $ingredientIds] AS matched,
                     [x IN ingredients WHERE NOT x.id IN $ingredientIds] AS missing
                OPTIONAL MATCH (owned:Ingredient)-[:SUBSTITUTE_FOR]->(needed:Ingredient)
                WHERE owned.id IN $ingredientIds AND needed IN missing
                WITH r, c, dietNodes, applianceNodes, ingredients, matched, missing,
                     count(DISTINCT needed) AS substitutedMissing
                WHERE size($ingredientIds) = 0 OR size(matched) > 0 OR substitutedMissing > 0
                RETURN r.id AS id, r.name AS name, r.description AS description,
                       r.cookTimeMinutes AS cookTimeMinutes, r.difficulty AS difficulty,
                       r.cookingMethod AS cookingMethod, coalesce(r.imageUrl, '') AS imageUrl,
                       coalesce(c.name, '') AS cuisine,
                       [x IN dietNodes | x.name] AS diets, [x IN applianceNodes | x.name] AS appliances,
                       size(matched) AS matchedCount, size(ingredients) AS totalCount,
                       size(missing) AS missingCount,
                       (size(missing) > 0 AND substitutedMissing = size(missing)) AS possibleWithSubstitutions
                ORDER BY CASE WHEN size(ingredients)=0 THEN 0 ELSE toFloat(size(matched))/size(ingredients) END DESC,
                         possibleWithSubstitutions DESC, r.cookTimeMinutes ASC, r.name ASC
                LIMIT 50
                """;
        Map<String,Object> params = new HashMap<>();
        params.put("ingredientIds", request.ingredientIds()); params.put("applianceIds", request.applianceIds());
        params.put("dietIds", request.dietIds()); params.put("cuisineId", Objects.requireNonNullElse(request.cuisineId(), ""));
        params.put("maxTime", request.maxCookTimeMinutes());
        return read(cypher, params, this::summary);
    }

    public RecipeDetail detail(String id, List<String> pantryIds) {
        String cypher = """
                MATCH (r:Recipe {id:$id})-[u:USES]->(i:Ingredient)
                OPTIONAL MATCH (r)-[:BELONGS_TO]->(c:Cuisine)
                OPTIONAL MATCH (r)-[:SUITABLE_FOR]->(d:Diet)
                OPTIONAL MATCH (r)-[:REQUIRES_APPLIANCE]->(a:Appliance)
                OPTIONAL MATCH (sub:Ingredient)-[s:SUBSTITUTE_FOR]->(i)
                WITH r, c, collect(DISTINCT d.name) AS diets, collect(DISTINCT a.name) AS appliances,
                     i, u, collect(DISTINCT {id:sub.id, name:sub.name, similarityScore:s.similarityScore,
                          note:s.note, owned:sub.id IN $pantryIds}) AS rawSubs
                RETURN r.id AS id, r.name AS name, r.description AS description,
                       r.cookTimeMinutes AS cookTimeMinutes, r.difficulty AS difficulty,
                       r.cookingMethod AS cookingMethod, coalesce(r.imageUrl,'') AS imageUrl,
                       coalesce(c.name,'') AS cuisine, diets, appliances, coalesce(r.instructions, []) AS instructions,
                       i.id AS ingredientId, i.name AS ingredientName, i.category AS category,
                       u.quantity AS quantity, u.unit AS unit, i.id IN $pantryIds AS available,
                       [x IN rawSubs WHERE x.id IS NOT NULL] AS substitutions
                ORDER BY i.name
                """;
        List<Record> rows = records(cypher, Map.of("id", id, "pantryIds", pantryIds));
        if (rows.isEmpty()) throw new ApiExceptions.NotFound("Recipe not found.");
        Record first = rows.getFirst();
        List<IngredientLine> ingredients = rows.stream().map(r -> new IngredientLine(
                r.get("ingredientId").asString(), r.get("ingredientName").asString(), r.get("category").asString(""),
                r.get("quantity").asDouble(), r.get("unit").asString(), r.get("available").asBoolean(),
                r.get("substitutions").asList(v -> { Map<String,Object> m=v.asMap(); return new Substitution(
                        Objects.toString(m.get("id"),""), Objects.toString(m.get("name"),""),
                        ((Number)m.getOrDefault("similarityScore",0d)).doubleValue(), Objects.toString(m.get("note"),""),
                        Boolean.TRUE.equals(m.get("owned"))); })) ).toList();
        return new RecipeDetail(first.get("id").asString(), first.get("name").asString(), first.get("description").asString(),
                first.get("cookTimeMinutes").asInt(), first.get("difficulty").asString(), first.get("cookingMethod").asString(),
                first.get("imageUrl").asString(), first.get("cuisine").asString(), first.get("diets").asList(v->v.asString()),
                first.get("appliances").asList(v->v.asString()), ingredients,
                first.get("instructions").asList(v->v.asString()));
    }

    public GraphResponse graph() {
        String nodesQuery = """
                MATCH (n) WHERE n:Recipe OR n:Ingredient OR n:Cuisine OR n:Diet OR n:Appliance OR n:Allergen
                WITH n ORDER BY CASE WHEN n:Recipe THEN 0 ELSE 1 END, n.name LIMIT 80
                RETURN n.id AS id, labels(n)[0] AS label, n.name AS name,
                       {description:coalesce(n.description,''), category:coalesce(n.category,'')} AS properties
                """;
        List<GraphNode> nodes = read(nodesQuery, Map.of(), r -> new GraphNode(r.get("id").asString(), r.get("label").asString(),
                r.get("name").asString(), r.get("properties").asMap()));
        Set<String> ids = new HashSet<>(nodes.stream().map(GraphNode::id).toList());
        String edgesQuery = """
                MATCH (a)-[rel:USES|BELONGS_TO|SUITABLE_FOR|REQUIRES_APPLIANCE|SUBSTITUTE_FOR|CONTAINS_ALLERGEN]->(b)
                WHERE a.id IN $ids AND b.id IN $ids
                RETURN a.id AS source, b.id AS target, type(rel) AS type, properties(rel) AS properties LIMIT 140
                """;
        List<GraphEdge> edges = read(edgesQuery, Map.of("ids", ids), r -> new GraphEdge(r.get("source").asString(),
                r.get("target").asString(), r.get("type").asString(), r.get("properties").asMap()));
        return new GraphResponse(nodes, edges);
    }

    private RecipeSummary summary(Record r) {
        int total=r.get("totalCount").asInt(), matched=r.get("matchedCount").asInt();
        int percentage=total == 0 ? 0 : (int)Math.round(matched * 100.0 / total);
        return new RecipeSummary(r.get("id").asString(), r.get("name").asString(), r.get("description").asString(),
                r.get("cookTimeMinutes").asInt(), r.get("difficulty").asString(), r.get("cookingMethod").asString(),
                r.get("imageUrl").asString(), r.get("cuisine").asString(), r.get("diets").asList(v->v.asString()),
                r.get("appliances").asList(v->v.asString()), percentage, matched, total, r.get("missingCount").asInt(),
                r.get("possibleWithSubstitutions").asBoolean());
    }

    private List<Record> records(String query, Map<String,Object> params) {
        try (Session session=driver.session()) { return session.executeRead(tx -> tx.run(query, params).list()); }
        catch (Neo4jException | IllegalStateException ex) { throw new ApiExceptions.DatabaseUnavailable(ex); }
    }
    private <T> List<T> read(String query, Map<String,Object> params, java.util.function.Function<Record,T> mapper) {
        return records(query, params).stream().map(mapper).toList();
    }
}
