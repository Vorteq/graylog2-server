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
package org.graylog.storage.elasticsearch7.views.export;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.export.ExportException;
import org.graylog.plugins.views.search.export.ExportMessagesCommand;
import org.graylog.plugins.views.search.export.SimpleMessageChunk;
import org.graylog.plugins.views.search.searchfilters.db.IgnoreSearchFilters;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;


public class ElasticsearchExportBackendIT extends ElasticsearchBaseTest {

    private IndexLookup indexLookup;
    private ElasticsearchExportBackend backend;

    private ElasticsearchExportITHelper helper;

    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    protected SearchServerInstance searchServer() {
        return this.elasticsearch;
    }

    @Before
    public void setUp() {
        indexLookup = mock(IndexLookup.class);
        backend = new ElasticsearchExportBackend(indexLookup, requestStrategy(), false, new IgnoreSearchFilters());
        helper = new ElasticsearchExportITHelper(indexLookup, backend);

    }

    private RequestStrategy requestStrategy() {
        final ExportClient exportClient = new ExportClient(elasticsearch.elasticsearchClient());
        return new SearchAfter(exportClient);
    }

    @Test
    public void usesCorrectIndicesAndStreams() {
        importFixture("messages.json");

        ExportMessagesCommand command = helper.commandBuilderWithAllTestDefaultStreams()
                .streams(ImmutableSet.of("stream-01", "stream-02"))
                .build();

        helper.mockIndexLookupFor(command, "graylog_0", "graylog_1");

        helper.runWithExpectedResultIgnoringSort(command, "timestamp,source,message",
                "graylog_0, 2015-01-01T01:00:00.000Z, source-1, Ha",
                "graylog_1, 2015-01-01T01:59:59.999Z, source-2, He",
                "graylog_0, 2015-01-01T04:00:00.000Z, source-2, Ho"
        );
    }

    @Test
    public void usesQueryString() {
        importFixture("messages.json");

        ExportMessagesCommand command = helper.commandBuilderWithAllTestDefaultStreams()
                .queryString(ElasticsearchQueryString.of("Ha Ho"))
                .build();

        helper.runWithExpectedResultIgnoringSort(command, "timestamp,source,message",
                "graylog_0, 2015-01-01T04:00:00.000Z, source-2, Ho",
                "graylog_0, 2015-01-01T01:00:00.000Z, source-1, Ha"
        );
    }

    @Test
    public void usesTimeRange() {
        importFixture("messages.json");

        ExportMessagesCommand command = helper.commandBuilderWithAllTestDefaultStreams()
                .timeRange(AbsoluteRange.create("2015-01-01T00:00:00.000Z", "2015-01-01T02:00:00.000Z"))
                .build();

        helper.runWithExpectedResultIgnoringSort(command, "timestamp,source,message",
                "graylog_1, 2015-01-01T01:59:59.999Z, source-2, He",
                "graylog_0, 2015-01-01T01:00:00.000Z, source-1, Ha"
        );
    }

    @Test
    public void usesFieldsInOrder() {
        importFixture("messages.json");

        ExportMessagesCommand command = helper.commandBuilderWithAllTestDefaultStreams()
                .fieldsInOrder("timestamp", "message")
                .build();

        helper.runWithExpectedResultIgnoringSort(command, "timestamp,message",
                "graylog_0, 2015-01-01T04:00:00.000Z, Ho",
                "graylog_0, 2015-01-01T03:00:00.000Z, Hi",
                "graylog_1, 2015-01-01T01:59:59.999Z, He",
                "graylog_0, 2015-01-01T01:00:00.000Z, Ha");
    }

    @Test
    public void marksFirstChunk() {
        importFixture("messages.json");

        ExportMessagesCommand command = helper.commandBuilderWithAllTestDefaultStreams().build();

        SimpleMessageChunk[] chunks = helper.collectChunksFor(command).toArray(new SimpleMessageChunk[0]);

        assertThat(chunks[0].isFirstChunk()).isTrue();
    }

    @Test
    public void failsWithLeadingHighlightQueryIfDisallowed() {
        importFixture("messages.json");

        ExportMessagesCommand command = helper.commandBuilderWithAllTestDefaultStreams().queryString(ElasticsearchQueryString.of("*a")).build();

        assertThatExceptionOfType(ExportException.class)
                .isThrownBy(() -> backend.run(command, chunk -> {}))
                .withCauseInstanceOf(ElasticsearchException.class);
    }

    @Test
    public void respectsResultLimitIfSet() {
        importFixture("messages.json");

        ExportMessagesCommand command = helper.commandBuilderWithAllTestDefaultStreams().chunkSize(1).limit(3).build();

        SimpleMessageChunk totalResult = helper.collectTotalResult(command);

        assertThat(totalResult.messages()).hasSize(3);
    }

    @Test
    public void deliversCompleteLastChunkIfLimitIsReached() {
        importFixture("messages.json");

        ExportMessagesCommand command = helper.commandBuilderWithAllTestDefaultStreams().chunkSize(2).limit(3).build();

        SimpleMessageChunk totalResult = helper.collectTotalResult(command);

        assertThat(totalResult.messages()).hasSize(4);
    }

    @Test
    public void resultsHaveAllMessageFields() {
        importFixture("messages.json");

        ExportMessagesCommand command = helper.commandBuilderWithAllTestDefaultStreams()
                .fieldsInOrder("timestamp", "message")
                .build();

        LinkedHashSet<SimpleMessageChunk> allChunks = helper.collectChunksFor(command);
        SimpleMessageChunk totalResult = allChunks.iterator().next();

        Set<String> allFieldsInResult = helper.actualFieldNamesFrom(totalResult);

        assertThat(allFieldsInResult).containsExactlyInAnyOrder(
                "gl2_message_id",
                "source",
                "message",
                "timestamp",
                "streams",
                "_id");
    }

    @Test
    public void sortsByTimestampAscending() {
        importFixture("messages.json");

        ExportMessagesCommand command = helper.commandBuilderWithAllTestDefaultStreams().build();

        helper.runWithExpectedResult(command, "timestamp,source,message",
                "graylog_0, 2015-01-01T01:00:00.000Z, source-1, Ha",
                "graylog_1, 2015-01-01T01:59:59.999Z, source-2, He",
                "graylog_0, 2015-01-01T03:00:00.000Z, source-1, Hi",
                "graylog_0, 2015-01-01T04:00:00.000Z, source-2, Ho");
    }

    @Test
    public void usesProvidedTimeZone() {
        importFixture("messages.json");

        ExportMessagesCommand command = helper.commandBuilderWithAllTestDefaultStreams()
                .timeZone(DateTimeZone.forID("Australia/Adelaide")) // UTC+9:30
                .build();

        helper.runWithExpectedResult(command, "timestamp,source,message",
                "graylog_0, 2015-01-01T11:30:00.000+10:30, source-1, Ha",
                "graylog_1, 2015-01-01T12:29:59.999+10:30, source-2, He",
                "graylog_0, 2015-01-01T13:30:00.000+10:30, source-1, Hi",
                "graylog_0, 2015-01-01T14:30:00.000+10:30, source-2, Ho");
    }


}
