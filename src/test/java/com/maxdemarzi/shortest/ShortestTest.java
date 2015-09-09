package com.maxdemarzi.shortest;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.test.server.HTTP;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertArrayEquals;

public class ShortestTest {
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()
            .withFixture(MODEL_STATEMENT)
            .withExtension("/v1", Service.class);

    @Test
    public void shouldFindShortestPathOne() {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query").toString(),
                QUERY_ONE_MAP);

        ArrayList actual = response.content();
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{ add(ONE_MAP); }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldFindShortestPathTwo() {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query").toString(),
                QUERY_TWO_MAP);

        ArrayList actual = response.content();
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(ONE_MAP);
            add(TWO_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldFindShortestPathThree() {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query").toString(),
                QUERY_THREE_MAP);

        ArrayList actual = response.content();
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(THREE_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldDealWithMissingEmails() {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query").toString(),
                QUERY_FOUR_MAP);

        ArrayList actual = response.content();
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(FOUR_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }
    public static final String MODEL_STATEMENT =
            new StringBuilder()
                    .append("CREATE (start:Email {email:'start@maxdemarzi.com'})")
                    .append("CREATE (one:Email {email:'one@maxdemarzi.com'})")
                    .append("CREATE (two:Email {email:'two@maxdemarzi.com'})")
                    .append("CREATE (three:Email {email:'three@maxdemarzi.com'})")
                    .append("CREATE (four:Email {email:'four@maxdemarzi.com'})")
                    .append("CREATE (five:Email {email:'five@maxdemarzi.com'})")
                    .append("CREATE (start)-[:CONNECTS]->(one)")
                    .append("CREATE (one)-[:CONNECTS]->(two)")
                    .append("CREATE (one)-[:CONNECTS]->(three)")
                    .append("CREATE (one)-[:CONNECTS]->(four)")
                    .append("CREATE (three)<-[:CONNECTS]-(five)")
                    .append("CREATE (four)<-[:CONNECTS]-(five)")
                    .toString();

    public static HashMap<String, Object> QUERY_ONE_MAP = new HashMap<String, Object>(){{
        put("center_email", "start@maxdemarzi.com");
        put("edge_emails", new ArrayList<String>() {{  add("one@maxdemarzi.com");} });
        put("length", 4);
    }};

    static HashMap<String, Object> ONE_MAP = new HashMap<String, Object>(){{
        put("email", "one@maxdemarzi.com");
        put("length", 1);
        put("count", 1);
    }};

    public static HashMap<String, Object> QUERY_TWO_MAP = new HashMap<String, Object>(){{
        put("center_email", "start@maxdemarzi.com");
        put("edge_emails", new ArrayList<String>() {{
            add("one@maxdemarzi.com");
            add("two@maxdemarzi.com");
        }});
        put("length", 4);
    }};

    static HashMap<String, Object> TWO_MAP = new HashMap<String, Object>(){{
        put("email", "two@maxdemarzi.com");
        put("length", 2);
        put("count", 1);
    }};

    public static HashMap<String, Object> QUERY_THREE_MAP = new HashMap<String, Object>(){{
        put("center_email", "start@maxdemarzi.com");
        put("edge_emails", new ArrayList<String>() {{
            add("five@maxdemarzi.com");
        }});
        put("length", 4);
    }};

    static HashMap<String, Object> THREE_MAP = new HashMap<String, Object>(){{
        put("email", "five@maxdemarzi.com");
        put("length", 3);
        put("count", 2);
    }};

    public static HashMap<String, Object> QUERY_FOUR_MAP = new HashMap<String, Object>(){{
        put("center_email", "start@maxdemarzi.com");
        put("edge_emails", new ArrayList<String>() {{
            add("five@maxdemarzi.com");
            add("six@maxdemarzi.com");
        }});
        put("length", 4);
    }};

    static HashMap<String, Object> FOUR_MAP = new HashMap<String, Object>(){{
        put("email", "five@maxdemarzi.com");
        put("length", 3);
        put("count", 2);
    }};
}
