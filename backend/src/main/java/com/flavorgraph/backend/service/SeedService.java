package com.flavorgraph.backend.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnProperty(name="app.seed-enabled", havingValue="true")
public class SeedService implements CommandLineRunner {
    private static final Logger log= LoggerFactory.getLogger(SeedService.class);
    private final Driver driver;
    public SeedService(Driver driver) { this.driver=driver; }

    private record Recipe(String id,String name,String description,int time,String difficulty,String method,String cuisine,
                          List<String> diets,List<String> appliances,List<String> ingredients) {}

    @Override public void run(String... args) {
        try (Session session=driver.session()) {
            constraints(session);
            seedTaxonomy(session);
            seedRecipes(session);
            seedSubstitutions(session);
            seedAllergens(session);
            log.info("FlavorGraph demo data seeded idempotently");
        }
    }

    private void constraints(Session s) {
        Map<String,String> queries=Map.of(
                "Recipe", "CREATE CONSTRAINT recipe_id IF NOT EXISTS FOR (n:Recipe) REQUIRE n.id IS UNIQUE",
                "Ingredient", "CREATE CONSTRAINT ingredient_id IF NOT EXISTS FOR (n:Ingredient) REQUIRE n.id IS UNIQUE",
                "Cuisine", "CREATE CONSTRAINT cuisine_id IF NOT EXISTS FOR (n:Cuisine) REQUIRE n.id IS UNIQUE",
                "Diet", "CREATE CONSTRAINT diet_id IF NOT EXISTS FOR (n:Diet) REQUIRE n.id IS UNIQUE",
                "Appliance", "CREATE CONSTRAINT appliance_id IF NOT EXISTS FOR (n:Appliance) REQUIRE n.id IS UNIQUE",
                "Allergen", "CREATE CONSTRAINT allergen_id IF NOT EXISTS FOR (n:Allergen) REQUIRE n.id IS UNIQUE");
        for (var entry:queries.entrySet()) {
            try { s.run(entry.getValue()).consume(); }
            catch (Exception ex) { log.info("Optional {} constraint was not created; continuing for CognoDB compatibility",entry.getKey()); }
        }
    }

    private void seedTaxonomy(Session s) {
        List<Map<String,Object>> ingredients = Arrays.stream(("chicken|Chicken|Protein,rice|Rice|Grain,tomato|Tomato|Produce,onion|Onion|Produce,"+
                "garlic|Garlic|Produce,potato|Potato|Produce,yogurt|Yogurt|Dairy,greek-yogurt|Greek Yogurt|Dairy,paneer|Paneer|Dairy,"+
                "tofu|Tofu|Protein,eggs|Eggs|Protein,bread|Bread|Bakery,pasta|Pasta|Grain,cheese|Cheese|Dairy,milk|Milk|Dairy,"+
                "coconut-milk|Coconut Milk|Pantry,bell-pepper|Bell Pepper|Produce,spinach|Spinach|Produce,chickpeas|Chickpeas|Protein,"+
                "lemon|Lemon|Produce,lime|Lime|Produce,olive-oil|Olive Oil|Pantry,butter|Butter|Dairy,basil|Basil|Herb,"+
                "cucumber|Cucumber|Produce,tortilla|Tortilla|Bakery,corn|Corn|Produce,carrot|Carrot|Produce,peas|Peas|Produce,"+
                "ginger|Ginger|Produce,soy-sauce|Soy Sauce|Pantry,flour|Flour|Baking,mozzarella|Mozzarella|Dairy,"+
                "parmesan|Parmesan|Dairy,mushroom|Mushroom|Produce,broccoli|Broccoli|Produce,avocado|Avocado|Produce,"+
                "beans|Black Beans|Protein,paprika|Paprika|Spice,cumin|Cumin|Spice,curry-powder|Curry Powder|Spice").split(","))
                .map(x->{String[] p=x.split("\\|"); return Map.<String,Object>of("id",p[0],"name",p[1],"category",p[2]);}).toList();
        write(s,"UNWIND $rows AS x MERGE (n:Ingredient {id:x.id}) SET n.name=x.name,n.category=x.category",ingredients);
        taxonomy(s,"Cuisine","indian|Indian,italian|Italian,mexican|Mexican,mediterranean|Mediterranean,american|American,asian|Asian");
        taxonomy(s,"Diet","vegetarian|Vegetarian,high-protein|High Protein,gluten-free|Gluten Free,dairy-free|Dairy Free");
        taxonomy(s,"Appliance","air-fryer|Air Fryer,oven|Oven,stovetop|Stovetop,microwave|Microwave,pressure-cooker|Pressure Cooker");
        taxonomy(s,"Allergen","dairy|Dairy,egg|Egg,wheat|Wheat,soy|Soy");
    }

    private void taxonomy(Session s,String label,String csv) {
        List<Map<String,Object>> rows=Arrays.stream(csv.split(",")).map(x->{String[] p=x.split("\\|");return Map.<String,Object>of("id",p[0],"name",p[1]);}).toList();
        String query=switch(label) {
            case "Cuisine" -> "UNWIND $rows AS x MERGE (n:Cuisine {id:x.id}) SET n.name=x.name";
            case "Diet" -> "UNWIND $rows AS x MERGE (n:Diet {id:x.id}) SET n.name=x.name";
            case "Appliance" -> "UNWIND $rows AS x MERGE (n:Appliance {id:x.id}) SET n.name=x.name";
            case "Allergen" -> "UNWIND $rows AS x MERGE (n:Allergen {id:x.id}) SET n.name=x.name";
            default -> throw new IllegalArgumentException("Unsupported taxonomy label");
        };
        write(s,query,rows);
    }

    private void seedRecipes(Session s) {
        List<Recipe> recipes=List.of(
                r("chicken-tikka-bowl","Chicken Tikka Bowl",30,"MEDIUM","ROAST","indian","high-protein","oven","chicken,rice,tomato,onion,yogurt,garlic"),
                r("paneer-tikka","Paneer Tikka",28,"EASY","ROAST","indian","vegetarian,high-protein","oven","paneer,yogurt,bell-pepper,onion,garlic"),
                r("vegetable-pulao","Vegetable Pulao",35,"EASY","SIMMER","indian","vegetarian,dairy-free","pressure-cooker","rice,carrot,peas,onion,ginger"),
                r("egg-masala","Egg Masala",30,"MEDIUM","SIMMER","indian","high-protein,gluten-free","stovetop","eggs,tomato,onion,garlic,ginger"),
                r("air-fryer-paneer-tikka","Air Fryer Paneer Tikka",22,"EASY","AIR_FRY","indian","vegetarian,high-protein","air-fryer","paneer,yogurt,bell-pepper,onion"),
                r("tomato-basil-pasta","Tomato Basil Pasta",20,"EASY","BOIL","italian","vegetarian","stovetop","pasta,tomato,garlic,basil,parmesan"),
                r("garlic-chicken-pasta","Garlic Chicken Pasta",28,"MEDIUM","SAUTE","italian","high-protein","stovetop","pasta,chicken,garlic,spinach,parmesan"),
                r("vegetable-pasta","Vegetable Pasta",25,"EASY","SAUTE","italian","vegetarian","stovetop","pasta,tomato,bell-pepper,mushroom,garlic"),
                r("chicken-rice-bowl","Chicken Rice Bowl",25,"EASY","SAUTE","mexican","high-protein,gluten-free","stovetop","chicken,rice,corn,beans,tomato,lime"),
                r("veggie-quesadilla","Veggie Quesadilla",18,"EASY","GRIDDLE","mexican","vegetarian","stovetop","tortilla,cheese,bell-pepper,corn,onion"),
                r("greek-chicken-bowl","Greek Chicken Bowl",30,"EASY","GRILL","mediterranean","high-protein,gluten-free","oven","chicken,rice,cucumber,tomato,greek-yogurt,lemon"),
                r("chickpea-salad","Chickpea Garden Salad",12,"EASY","NO_COOK","mediterranean","vegetarian,dairy-free,gluten-free","microwave","chickpeas,cucumber,tomato,onion,lemon,olive-oil"),
                r("air-fryer-garlic-chicken","Air Fryer Garlic Chicken",24,"EASY","AIR_FRY","american","high-protein,gluten-free","air-fryer","chicken,garlic,olive-oil,paprika,lemon"),
                r("crispy-air-fryer-potatoes","Crispy Air Fryer Potatoes",25,"EASY","AIR_FRY","american","vegetarian,dairy-free,gluten-free","air-fryer","potato,olive-oil,garlic,paprika"),
                r("air-fryer-chicken-potato-bowl","Air Fryer Chicken & Potato Bowl",30,"EASY","AIR_FRY","american","high-protein,gluten-free","air-fryer","chicken,potato,garlic,bell-pepper,olive-oil"),
                r("omelette","Garden Omelette",12,"EASY","PAN_FRY","american","vegetarian,high-protein,gluten-free","stovetop","eggs,spinach,tomato,onion,cheese"),
                r("grilled-cheese","Golden Grilled Cheese",10,"EASY","GRIDDLE","american","vegetarian","stovetop","bread,cheese,butter"),
                r("vegetable-fried-rice","Vegetable Fried Rice",20,"EASY","STIR_FRY","asian","vegetarian,dairy-free","stovetop","rice,eggs,carrot,peas,soy-sauce,onion"),
                r("spinach-paneer","Spinach Paneer",32,"MEDIUM","SIMMER","indian","vegetarian,high-protein,gluten-free","stovetop","paneer,spinach,tomato,onion,garlic"),
                r("mushroom-risotto","Mushroom Rice Skillet",35,"MEDIUM","SIMMER","italian","vegetarian,gluten-free","stovetop","rice,mushroom,onion,parmesan,butter"),
                r("black-bean-tacos","Black Bean Tacos",20,"EASY","SAUTE","mexican","vegetarian,dairy-free","stovetop","tortilla,beans,corn,tomato,avocado,lime"),
                r("broccoli-cheese-potato","Broccoli Cheese Potato",18,"EASY","MICROWAVE","american","vegetarian,gluten-free","microwave","potato,broccoli,cheese,milk"),
                r("coconut-chickpea-curry","Coconut Chickpea Curry",25,"EASY","SIMMER","indian","vegetarian,dairy-free,gluten-free","stovetop","chickpeas,coconut-milk,tomato,onion,curry-powder"),
                r("tofu-vegetable-bowl","Tofu Vegetable Bowl",26,"EASY","STIR_FRY","asian","vegetarian,dairy-free","stovetop","tofu,rice,broccoli,carrot,soy-sauce,ginger")
        );
        for (Recipe r:recipes) {
            Map<String,Object> p=new HashMap<>(); p.put("id",r.id);p.put("name",r.name);p.put("description",r.description);p.put("time",r.time);p.put("difficulty",r.difficulty);p.put("method",r.method);p.put("instructions",instructions(r));
            s.run("MERGE (r:Recipe {id:$id}) SET r.name=$name,r.description=$description,r.cookTimeMinutes=$time,r.difficulty=$difficulty,r.cookingMethod=$method,r.instructions=$instructions",p).consume();
            link(s,r.id,List.of(r.cuisine),"BELONGS_TO"); link(s,r.id,r.diets,"SUITABLE_FOR"); link(s,r.id,r.appliances,"REQUIRES_APPLIANCE");
            int ix=0; for(String ingredient:r.ingredients) { double qty=switch(ingredient){case "chicken","paneer","tofu"->250;case "rice","pasta"->180;default->1;};String unit=qty>1?"g":"portion";
                s.run("MATCH (r:Recipe {id:$recipe}),(i:Ingredient {id:$ingredient}) MERGE (r)-[u:USES]->(i) SET u.quantity=$quantity,u.unit=$unit",
                        Map.of("recipe",r.id,"ingredient",ingredient,"quantity",qty,"unit",unit)).consume(); ix++; }
        }
    }

    private Recipe r(String id,String name,int time,String difficulty,String method,String cuisine,String diets,String appliances,String ingredients) {
        return new Recipe(id,name,"A practical, flavor-forward "+name.toLowerCase()+" for an easy meal.",time,difficulty,method,cuisine,List.of(diets.split(",")),List.of(appliances.split(",")),List.of(ingredients.split(",")));
    }
    private List<String> instructions(Recipe recipe) {
        String prep="Gather and prepare all ingredients: wash produce, measure pantry items, and cut everything into even pieces.";
        String finish="Taste, adjust the seasoning if needed, and serve "+recipe.name+" while warm.";
        return switch(recipe.method) {
            case "AIR_FRY" -> List.of(prep,"Preheat the air fryer to 190°C (375°F) for 3 minutes.","Toss the prepared ingredients with the oil and seasonings until evenly coated.","Arrange in a single layer and air-fry, turning halfway through, until browned and cooked through.",finish);
            case "ROAST", "GRILL" -> List.of(prep,"Preheat the oven to 200°C (400°F) and prepare a lined tray.","Combine the main ingredients with aromatics, seasonings, and any marinade.","Cook until browned and the center is fully cooked, turning once for even color.",finish);
            case "SIMMER" -> List.of(prep,"Warm a deep pan over medium heat and cook the onion and aromatics until fragrant.","Add the spices and remaining main ingredients, stirring to coat them evenly.","Add enough liquid for the sauce, cover, and simmer gently until tender.",finish);
            case "STIR_FRY", "SAUTE" -> List.of(prep,"Heat a wide pan over medium-high heat with a small amount of oil.","Cook the protein or firm vegetables first, stirring until nearly done.","Add the remaining vegetables and sauce, then toss until hot and evenly coated.",finish);
            case "BOIL" -> List.of(prep,"Bring a large pot of salted water to a boil.","Cook the pasta until just tender, reserving a little cooking water before draining.","Build the sauce in a pan, add the pasta, and toss with enough reserved water to coat.",finish);
            case "PAN_FRY", "GRIDDLE" -> List.of(prep,"Preheat a lightly greased skillet or griddle over medium heat.","Assemble or combine the prepared ingredients in an even layer.","Cook until golden underneath, turn carefully, and finish the second side.",finish);
            case "MICROWAVE" -> List.of(prep,"Place the main ingredient in a microwave-safe dish and cover loosely.","Microwave in short intervals until tender, turning or stirring between intervals.","Add the remaining toppings and heat briefly until warmed through.",finish);
            case "NO_COOK" -> List.of(prep,"Combine the vegetables, protein, and herbs in a large serving bowl.","Whisk the citrus and oil with the seasonings to make the dressing.","Pour over the bowl and toss gently until everything is evenly coated.","Rest for 5 minutes so the flavors combine, then serve.");
            default -> List.of(prep,"Cook the ingredients using the listed method until tender and fully cooked.",finish);
        };
    }
    private void link(Session s,String recipe,List<String> ids,String rel) {
        String query=switch(rel) {
            case "BELONGS_TO" -> "MATCH (r:Recipe {id:$recipe}) UNWIND $ids AS id MATCH (n:Cuisine {id:id}) MERGE (r)-[:BELONGS_TO]->(n)";
            case "SUITABLE_FOR" -> "MATCH (r:Recipe {id:$recipe}) UNWIND $ids AS id MATCH (n:Diet {id:id}) MERGE (r)-[:SUITABLE_FOR]->(n)";
            case "REQUIRES_APPLIANCE" -> "MATCH (r:Recipe {id:$recipe}) UNWIND $ids AS id MATCH (n:Appliance {id:id}) MERGE (r)-[:REQUIRES_APPLIANCE]->(n)";
            default -> throw new IllegalArgumentException("Unsupported recipe relationship");
        };
        s.run(query,Map.of("recipe",recipe,"ids",ids)).consume();
    }
    private void seedSubstitutions(Session s) {
        String rows="greek-yogurt|yogurt|0.94|Use the same amount for a thicker result,coconut-milk|milk|0.82|Adds coconut flavor,olive-oil|butter|0.72|Best for sautéing or roasting,tofu|paneer|0.80|Texture varies by preparation,lime|lemon|0.90|Similar citrus brightness,mozzarella|cheese|0.88|Melts especially well,parmesan|cheese|0.75|Stronger and saltier,greek-yogurt|milk|0.64|Thin with water if needed,bread|tortilla|0.55|Useful for handheld fillings,chickpeas|beans|0.70|A mild legume alternative,tomato|bell-pepper|0.45|Adds moisture and color,spinach|basil|0.40|Different flavor but fresh green note";
        for(String row:rows.split(",")){String[] p=row.split("\\|");s.run("MATCH (a:Ingredient {id:$a}),(b:Ingredient {id:$b}) MERGE (a)-[x:SUBSTITUTE_FOR]->(b) SET x.similarityScore=$score,x.note=$note",Map.of("a",p[0],"b",p[1],"score",Double.parseDouble(p[2]),"note",p[3])).consume();}
    }
    private void seedAllergens(Session s) {
        Map<String,List<String>> map=Map.of("dairy",List.of("milk","cheese","yogurt","greek-yogurt","paneer","butter","mozzarella","parmesan"),"egg",List.of("eggs"),"wheat",List.of("bread","pasta","flour","tortilla"),"soy",List.of("tofu","soy-sauce"));
        map.forEach((allergen,ingredients)->s.run("UNWIND $ingredients AS id MATCH (i:Ingredient {id:id}),(a:Allergen {id:$allergen}) MERGE (i)-[:CONTAINS_ALLERGEN]->(a)",Map.of("ingredients",ingredients,"allergen",allergen)).consume());
    }
    private void write(Session s,String query,List<Map<String,Object>> rows){s.run(query,Map.of("rows",rows)).consume();}
}
