package com.shopverse.shared.web.error;

import com.shopverse.shared.exception.BusinessRuleViolationException;
import com.shopverse.shared.exception.ResourceConflictException;
import com.shopverse.shared.exception.ResourceNotFoundException;
import com.shopverse.shared.web.CorrelationId;
import com.shopverse.shared.web.RequestCorrelationFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({
    GlobalExceptionHandler.class,
    RequestCorrelationFilter.class,
    GlobalExceptionHandlerTest.ApiErrorTestController.class
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnStructuredValidationProblem() throws Exception {
        mockMvc.perform(
                post("/test/api/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                        CorrelationId.HEADER_NAME,
                        "manual-validation-test-001"
                    )
                    .content("""
                                        {
                                          "name": "",
                                          "quantity": 0
                                        }
                                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                header().string(
                    CorrelationId.HEADER_NAME,
                    "manual-validation-test-001"
                )
            )
            .andExpect(
                content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_PROBLEM_JSON
                )
            )
            .andExpect(
                jsonPath("$.title")
                    .value("Validation failed")
            )
            .andExpect(
                jsonPath("$.status")
                    .value(400)
            )
            .andExpect(
                jsonPath("$.code")
                    .value("VALIDATION_FAILED")
            )
            .andExpect(
                jsonPath("$.correlationId")
                    .value("manual-validation-test-001")
            )
            .andExpect(
                jsonPath("$.instance")
                    .value("/test/api/validate")
            )
            .andExpect(
                jsonPath("$.timestamp")
                    .exists()
            )
            .andExpect(
                jsonPath("$.violations.length()")
                    .value(2)
            )
            .andExpect(
                jsonPath("$.violations[0].field")
                    .value("name")
            )
            .andExpect(
                jsonPath("$.violations[0].message")
                    .value("name must not be blank")
            )
            .andExpect(
                jsonPath("$.violations[1].field")
                    .value("quantity")
            )
            .andExpect(
                jsonPath("$.violations[1].message")
                    .value("quantity must be at least 1")
            );
    }

    @Test
    void shouldReturnSafeProblemForMalformedJson() throws Exception {
        mockMvc.perform(
                post("/test/api/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                        CorrelationId.HEADER_NAME,
                        "malformed-request-001"
                    )
                    .content("""
                                        {
                                          "name": "Phone",
                                          "quantity":
                                        }
                                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.title")
                    .value("Malformed request")
            )
            .andExpect(
                jsonPath("$.code")
                    .value("MALFORMED_REQUEST")
            )
            .andExpect(
                jsonPath("$.detail")
                    .value(
                        "The request body is missing, malformed, or contains invalid data."
                    )
            )
            .andExpect(
                jsonPath("$.correlationId")
                    .value("malformed-request-001")
            );
    }

    @Test
    void shouldHideInternalExceptionDetails() throws Exception {
        mockMvc.perform(
                get("/test/api/unexpected")
                    .header(
                        CorrelationId.HEADER_NAME,
                        "unexpected-error-001"
                    )
            )
            .andExpect(status().isInternalServerError())
            .andExpect(
                jsonPath("$.title")
                    .value("Internal server error")
            )
            .andExpect(
                jsonPath("$.code")
                    .value("INTERNAL_SERVER_ERROR")
            )
            .andExpect(
                jsonPath("$.detail")
                    .value("An unexpected error occurred.")
            )
            .andExpect(
                jsonPath("$.correlationId")
                    .value("unexpected-error-001")
            )
            .andExpect(
                jsonPath("$.stackTrace")
                    .doesNotExist()
            )
            .andExpect(
                jsonPath("$.exception")
                    .doesNotExist()
            );
    }

    @Test
    void shouldReturnResourceNotFoundProblem() throws Exception {
        mockMvc.perform(
                get("/test/api/not-found")
                    .header(
                        CorrelationId.HEADER_NAME,
                        "not-found-test-001"
                    )
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.title")
                    .value("Resource not found")
            )
            .andExpect(
                jsonPath("$.status")
                    .value(404)
            )
            .andExpect(
                jsonPath("$.code")
                    .value("RESOURCE_NOT_FOUND")
            )
            .andExpect(
                jsonPath("$.detail")
                    .value(
                        "Product with id 'product-101' was not found."
                    )
            )
            .andExpect(
                jsonPath("$.correlationId")
                    .value("not-found-test-001")
            )
            .andExpect(
                jsonPath("$.instance")
                    .value("/test/api/not-found")
            );
    }

    @Test
    void shouldReturnResourceConflictProblem() throws Exception {
        mockMvc.perform(
                get("/test/api/conflict")
                    .header(
                        CorrelationId.HEADER_NAME,
                        "conflict-test-001"
                    )
            )
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.title")
                    .value("Resource conflict")
            )
            .andExpect(
                jsonPath("$.status")
                    .value(409)
            )
            .andExpect(
                jsonPath("$.code")
                    .value("RESOURCE_CONFLICT")
            )
            .andExpect(
                jsonPath("$.detail")
                    .value(
                        "A customer with this email already exists."
                    )
            )
            .andExpect(
                jsonPath("$.correlationId")
                    .value("conflict-test-001")
            );
    }

    @Test
    void shouldReturnBusinessRuleViolationProblem() throws Exception {
        mockMvc.perform(
                get("/test/api/business-rule")
                    .header(
                        CorrelationId.HEADER_NAME,
                        "business-rule-test-001"
                    )
            )
            .andExpect(status().is(422))
            .andExpect(
                jsonPath("$.title")
                    .value("Business rule violation")
            )
            .andExpect(
                jsonPath("$.status")
                    .value(422)
            )
            .andExpect(
                jsonPath("$.code")
                    .value("BUSINESS_RULE_VIOLATION")
            )
            .andExpect(
                jsonPath("$.detail")
                    .value(
                        "An empty cart cannot be checked out."
                    )
            )
            .andExpect(
                jsonPath("$.correlationId")
                    .value("business-rule-test-001")
            );
    }

    @RestController
    @RequestMapping("/test/api")
    public static class ApiErrorTestController {

        @PostMapping("/validate")
        ResponseEntity<Void> validate(
            @Valid @RequestBody TestRequest request
        ) {
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/unexpected")
        String unexpected() {
            throw new IllegalStateException(
                "Sensitive internal implementation information"
            );
        }

        @GetMapping("/not-found")
        String notFound() {
            throw new ResourceNotFoundException(
                "Product with id 'product-101' was not found."
            );
        }

        @GetMapping("/conflict")
        String conflict() {
            throw new ResourceConflictException(
                "A customer with this email already exists."
            );
        }

        @GetMapping("/business-rule")
        String businessRuleViolation() {
            throw new BusinessRuleViolationException(
                "An empty cart cannot be checked out."
            );
        }
    }

    record TestRequest(

        @NotBlank(message = "name must not be blank")
        String name,

        @Min(
            value = 1,
            message = "quantity must be at least 1"
        )
        int quantity
    ) {
    }
}
