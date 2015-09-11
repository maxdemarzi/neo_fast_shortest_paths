package com.maxdemarzi.shortest;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.test.server.HTTP;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ShortestTest {

    final static ObjectMapper mapper = new ObjectMapper();

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
    public void shouldDealWithMissingEdgeEmail() {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query").toString(),
                QUERY_FOUR_MAP);

        ArrayList actual = response.content();
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(FOUR_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldDealWithMissingCenterEmail() {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query").toString(),
                QUERY_NO_CENTER_EMAIL_MAP);

        ArrayList actual = response.content();
        ArrayList<HashMap> expected = new ArrayList<HashMap>();
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldIgnoreWithNoPath() {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query").toString(),
                QUERY_NO_PATH_MAP);

        ArrayList actual = response.content();
        ArrayList<HashMap> expected = new ArrayList<HashMap>();
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldStreamFindShortestPathOne() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_streaming").toString(),
                QUERY_ONE_MAP);

        String raw = response.rawContent();
        Map<String,Object> actual = mapper.readValue(raw, Map.class);
        assertEquals(ONE_MAP, actual);
    }

    @Test
    public void shouldStreamFindShortestPathTwo() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_streaming").toString(),
                QUERY_TWO_MAP);

        ArrayList actual = parseNewlineSeparated(response);

        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(ONE_MAP);
            add(TWO_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldFindShortestPathByCountersOne() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_counters").toString(),
                QUERY_ONE_MAP);

        ArrayList actual = parseNewlineSeparated(response);
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{ add(ONE_MAP); }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldFindShortestPathByCountersTwo() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_counters").toString(),
                QUERY_TWO_MAP);

        ArrayList actual = parseNewlineSeparated(response);
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(ONE_MAP);
            add(TWO_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());

    }

    @Test
    public void shouldFindShortestPathByCountersThree() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_counters").toString(),
                QUERY_THREE_MAP);

        ArrayList actual = parseNewlineSeparated(response);
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(THREE_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());

    }

    @Test
    public void shouldDealWithMissingEmailsByCounters() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_counters").toString(),
                QUERY_FOUR_MAP);

        ArrayList actual = parseNewlineSeparated(response);
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(FOUR_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldFindShortestPathByCountersFive() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_counters").toString(),
                QUERY_FIVE_MAP);

        ArrayList actual = parseNewlineSeparated(response);
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(FIVE_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldFindShortestPathByCountersOne2() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_counters2").toString(),
                QUERY_ONE_MAP);

        ArrayList actual = parseNewlineSeparated(response);
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{ add(ONE_MAP); }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldFindShortestPathByCountersTwo2() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_counters2").toString(),
                QUERY_TWO_MAP);

        ArrayList actual = parseNewlineSeparated(response);
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(ONE_MAP);
            add(TWO_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());

    }

    @Test
    public void shouldFindShortestPathByCountersThree2() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_counters2").toString(),
                QUERY_THREE_MAP);

        ArrayList actual = parseNewlineSeparated(response);
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(THREE_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());

    }

    @Test
    public void shouldDealWithMissingEmailsByCounters2() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_counters2").toString(),
                QUERY_FOUR_MAP);

        ArrayList actual = parseNewlineSeparated(response);
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(FOUR_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void shouldFindShortestPathByCountersFive2() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/v1/service/query_counters2").toString(),
                QUERY_FIVE_MAP);

        ArrayList actual = parseNewlineSeparated(response);
        ArrayList<HashMap> expected = new ArrayList<HashMap>() {{
            add(FIVE_MAP);
        }};
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    private ArrayList parseNewlineSeparated(HTTP.Response response) throws Exception {
        String raw = response.rawContent();
        String[] lines = raw.split("\n");

        ArrayList actual = new ArrayList();
        for (String line : lines) {
            actual.add(mapper.readValue(line, Map.class));
        }

        return actual;
    }

    public static final String MODEL_STATEMENT =
            new StringBuilder()
                    .append("CREATE (start:Email {email:'start@maxdemarzi.com'})")
                    .append("CREATE (one:Email {email:'one@maxdemarzi.com'})")
                    .append("CREATE (two:Email {email:'two@maxdemarzi.com'})")
                    .append("CREATE (three:Email {email:'three@maxdemarzi.com'})")
                    .append("CREATE (four:Email {email:'four@maxdemarzi.com'})")
                    .append("CREATE (five:Email {email:'five@maxdemarzi.com'})")
                    .append("CREATE (nope:Email {email:'unconnected@maxdemarzi.com'})")
                    .append("CREATE (six:Email {email:'six@maxdemarzi.com'})")
                    .append("CREATE (seven:Email {email:'seven@maxdemarzi.com'})")
                    .append("CREATE (eight:Email {email:'eight@maxdemarzi.com'})")
                    .append("CREATE (start)-[:CONNECTS]->(one)")
                    .append("CREATE (one)-[:CONNECTS]->(two)")
                    .append("CREATE (one)-[:CONNECTS]->(three)")
                    .append("CREATE (one)-[:CONNECTS]->(four)")
                    .append("CREATE (three)<-[:CONNECTS]-(five)")
                    .append("CREATE (four)<-[:CONNECTS]-(five)")
                    .append("CREATE (two)-[:CONNECTS]->(six)")
                    .append("CREATE (six)-[:CONNECTS]->(seven)")
                    .append("CREATE (seven)-[:CONNECTS]->(eight)")
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
            add("sixty@maxdemarzi.com");
        }});
        put("length", 4);
    }};

    public static HashMap<String, Object> QUERY_NO_CENTER_EMAIL_MAP = new HashMap<String, Object>(){{
        put("center_email", "missing@maxdemarzi.com");
        put("edge_emails", new ArrayList<String>() {{
            add("five@maxdemarzi.com");
            add("start@maxdemarzi.com");
        }});
        put("length", 4);
    }};

    static HashMap<String, Object> FOUR_MAP = new HashMap<String, Object>(){{
        put("email", "five@maxdemarzi.com");
        put("length", 3);
        put("count", 2);
    }};

    public static HashMap<String, Object> QUERY_NO_PATH_MAP = new HashMap<String, Object>(){{
        put("center_email", "start@maxdemarzi.com");
        put("edge_emails", new ArrayList<String>() {{  add("unconnected@maxdemarzi.com");} });
        put("length", 4);
    }};

    public static HashMap<String, Object> QUERY_FIVE_MAP = new HashMap<String, Object>(){{
        put("center_email", "start@maxdemarzi.com");
        put("edge_emails", new ArrayList<String>() {{
            add("seven@maxdemarzi.com");
            add("eight@maxdemarzi.com");
        }});
        put("length", 4);
    }};

    static HashMap<String, Object> FIVE_MAP = new HashMap<String, Object>(){{
        put("email", "seven@maxdemarzi.com");
        put("length", 4);
        put("count", 1);
    }};
}
