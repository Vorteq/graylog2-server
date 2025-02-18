/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.rest.resources.system.field_types;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;

public record FieldTypeChangeRequest(@NotNull @NotEmpty
                                     @JsonProperty("index_sets")
                                     Set<String> indexSetsIds,
                                     @NotNull @NotEmpty
                                     @JsonProperty("field")
                                     String fieldName,
                                     @NotNull @NotEmpty
                                     @JsonProperty("type")
                                     String type,
                                     @JsonProperty("rotate")
                                     boolean rotateImmediately) {

}
