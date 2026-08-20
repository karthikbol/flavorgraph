package com.flavorgraph.backend.service;

import com.flavorgraph.backend.dto.ApiDtos.*;
import com.flavorgraph.backend.repository.FlavorGraphRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlavorGraphServiceTest {
    @Test void delegatesSearchWithoutChangingPantryFilters() {
        RecipeSummary expected=new RecipeSummary("r","Recipe","Description",20,"EASY","AIR_FRY","","Indian",List.of(),List.of("Air Fryer"),80,4,5,1,true);
        class StubRepository extends FlavorGraphRepository {
            RecipeSearchRequest received;
            StubRepository() { super(null); }
            @Override public List<RecipeSummary> search(RecipeSearchRequest request) { received=request; return List.of(expected); }
        }
        StubRepository repository=new StubRepository();
        FlavorGraphService service=new FlavorGraphService(repository);
        RecipeSearchRequest request=new RecipeSearchRequest(List.of("chicken"),List.of("air-fryer"),"indian",List.of(),30);
        assertEquals(List.of(expected),service.search(request));
        assertSame(request,repository.received);
    }

    @Test void normalizesNullCollectionsAtDtoBoundary() {
        RecipeSearchRequest request=new RecipeSearchRequest(null,null,null,null,30);
        assertTrue(request.ingredientIds().isEmpty());
        assertTrue(request.applianceIds().isEmpty());
        assertTrue(request.dietIds().isEmpty());
    }
}
