/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.watcher.input.simple;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.watcher.input.ExecutableInput;
import org.elasticsearch.watcher.input.Input;
import org.elasticsearch.watcher.input.InputFactory;
import org.elasticsearch.watcher.watch.Payload;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 */
public class SimpleInputTests extends ElasticsearchTestCase {

    @Test
    public void textExecute() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("foo", "bar");
        data.put("baz", new ArrayList<String>() );
        ExecutableInput staticInput = new ExecutableSimpleInput(new SimpleInput(new Payload.Simple(data)), logger);

        Input.Result staticResult = staticInput.execute(null);
        assertEquals(staticResult.payload().data().get("foo"), "bar");
        List baz = (List)staticResult.payload().data().get("baz");
        assertTrue(baz.isEmpty());
    }


    @Test
    public void testParser_Valid() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("foo", "bar");
        data.put("baz", new ArrayList<String>());

        XContentBuilder jsonBuilder = jsonBuilder().value(data);
        InputFactory parser = new SimpleInputFactory(Settings.builder().build());
        XContentParser xContentParser = JsonXContent.jsonXContent.createParser(jsonBuilder.bytes());
        xContentParser.nextToken();
        ExecutableInput input = parser.parseExecutable("_id", xContentParser);
        assertEquals(input.type(), SimpleInput.TYPE);


        Input.Result staticResult = input.execute(null);
        assertEquals(staticResult.payload().data().get("foo"), "bar");
        List baz = (List)staticResult.payload().data().get("baz");
        assertTrue(baz.isEmpty());
    }


    @Test(expected = ElasticsearchParseException.class)
    public void testParser_Invalid() throws Exception {

        XContentBuilder jsonBuilder = jsonBuilder().value("just a string");

        InputFactory parser = new SimpleInputFactory(Settings.builder().build());
        XContentParser xContentParser = JsonXContent.jsonXContent.createParser(jsonBuilder.bytes());
        xContentParser.nextToken();
        parser.parseInput("_id", xContentParser);
        fail("[simple] input parse should fail with an InputException for an empty json object");
    }

}
