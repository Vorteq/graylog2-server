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
package org.graylog.plugins.pipelineprocessor.functions.lookup;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;

import javax.inject.Inject;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class LookupValue extends AbstractFunction<Object> {

    public static final String NAME = "lookup_value";

    private final ParameterDescriptor<String, LookupTableService.Function> lookupTableParam;
    private final ParameterDescriptor<Object, Object> keyParam;
    private final ParameterDescriptor<Object, Object> defaultParam;

    @Inject
    public LookupValue(LookupTableService lookupTableService) {
        lookupTableParam = string("lookup_table", LookupTableService.Function.class)
                .description("The existing lookup table to use to lookup the given key")
                .transform(tableName -> lookupTableService.newBuilder().lookupTable(tableName).build())
                .build();
        keyParam = object("key").ruleBuilderVariable()
                .description("The key to lookup in the table")
                .build();
        defaultParam = object("default")
                .description("The default single value that should be used if there is no lookup result")
                .optional()
                .build();
    }

    @Override
    public Object evaluate(FunctionArgs args, EvaluationContext context) {
        Object key = keyParam.required(args, context);
        if (key == null) {
            return defaultParam.optional(args, context).orElse(null);
        }
        LookupTableService.Function table = lookupTableParam.required(args, context);
        if (table == null) {
            return defaultParam.optional(args, context).orElse(null);
        }
        LookupResult result = table.lookup(key);
        if (result == null || result.isEmpty()) {
            return defaultParam.optional(args, context).orElse(null);
        }
        return result.singleValue();
    }

    @Override
    public FunctionDescriptor<Object> descriptor() {
        //noinspection unchecked
        return FunctionDescriptor.builder()
                .name(NAME)
                .description("Looks up a single value in the named lookup table.")
                .params(lookupTableParam, keyParam, defaultParam)
                .returnType(Object.class)
                .ruleBuilderEnabled()
                .ruleBuilderName("Lookup single value")
                .ruleBuilderTitle("Lookup single value in '${lookup_table}' using '${key}'<#if $default??> (default: '${default}')</#if>")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.LOOKUP)
                .build();
    }
}
