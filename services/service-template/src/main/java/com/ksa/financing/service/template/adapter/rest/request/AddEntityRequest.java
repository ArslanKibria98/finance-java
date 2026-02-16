package com.ksa.financing.service.template.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for adding an entity to an example aggregate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add an entity to an example aggregate")
public class AddEntityRequest {

    @NotBlank(message = "Entity name is required")
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    @Schema(description = "Name of the entity", example = "Entity Name", required = true)
    private String name;

    @NotBlank(message = "Entity value is required")
    @Schema(description = "Value of the entity", example = "Entity Value", required = true)
    private String value;
}