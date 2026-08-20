package com.flavorgraph.backend.controller;

import com.flavorgraph.backend.exception.ApiExceptions;
import com.flavorgraph.backend.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    @Test void mapsDatabaseFailureToSafeResponse() {
        var response=new GlobalExceptionHandler().database(new ApiExceptions.DatabaseUnavailable(new RuntimeException("secret detail")));
        assertEquals("DATABASE_UNAVAILABLE",response.code());
        assertEquals("FlavorGraph is temporarily unable to reach its recipe database.",response.message());
    }
}
