package com.maxdemarzi.shortest;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.*;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.maxdemarzi.shortest.Validators.getValidQueryInput;

@Path("/service")
public class Service {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static GraphDatabaseService db;

    public Service(@Context GraphDatabaseService graphDatabaseService) {
        db = graphDatabaseService;
    }

    private static final LoadingCache<String, Long> emails = CacheBuilder.newBuilder()
            .maximumSize(1_000_000)
            .build(
                    new CacheLoader<String, Long>() {
                        public Long load(String email) throws Exception {
                            return getEmailNodeId(email);
                        }
                    });

    private static Long getEmailNodeId(String email) throws Exception{
        final Node node = db.findNode(Labels.Email, "email", email);
        if (node != null) {
            return node.getId();
        } else {
            throw new Exception("Email not found");
        }
    }

    @GET
    @Path("/helloworld")
    public Response helloWorld() throws IOException {
        Map<String, String> results = new HashMap<String,String>(){{
            put("hello","world");
        }};
        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    /**
     * JSON formatted body requires:
     *  center_email: An email address
     *  edge_emails: An Array of email addresses
     *  length: An integer representing the maximum traversal search length
     */
    @POST
    @Path("/query")
    public Response query(String body, @Context GraphDatabaseService db) throws IOException, ExecutionException {
        ArrayList<HashMap> results = new ArrayList<>();
        ArrayList<Node> edgeEmailNodes = new ArrayList<>();

        // Validate our input or exit right away
        HashMap input = getValidQueryInput(body);

        try (Transaction tx = db.beginTx()) {
            final Node centerNode;
            try {
                centerNode = db.getNodeById(emails.get((String) input.get("center_email")));
            } catch (ExecutionException e) {
                throw Exceptions.invalidCenterEmailParameter;
            }
            for (String edgeEmail : (ArrayList<String>)input.get("edge_emails")) {
                Long id;
                try {
                    id = emails.get(edgeEmail);
                } catch (Exception e) {
                    continue;
                }
                final Node edgeEmailNode = db.getNodeById(id);
                edgeEmailNodes.add(edgeEmailNode);
            }

            PathExpander<?> expander =  PathExpanders.allTypesAndDirections();
            PathFinder<org.neo4j.graphdb.Path> shortestPath = GraphAlgoFactory.shortestPath(expander, (int)input.get("length"));

            for (Node edgeEmail : edgeEmailNodes ) {
                HashMap<String, Object> result = new HashMap<>();
                int length = 0;
                int[] count = {0};
                for ( org.neo4j.graphdb.Path path : shortestPath.findAllPaths( centerNode, edgeEmail ) )
                {
                    length = path.length();
                    count[0]++;
                }
                result.put("email", edgeEmail.getProperty("email", ""));
                result.put("length", length);
                result.put("count", count[0]);

                results.add(result);
            }
        }
        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    /**
     * JSON formatted body requires:
     *  center_email: An email address
     *  edge_emails: An Array of email addresses
     *  length: An integer representing the maximum traversal search length
     */
    @POST
    @Path("/query2")
    public Response query2(String body, @Context GraphDatabaseService db) throws IOException, ExecutionException {

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                JsonGenerator jg = objectMapper.getJsonFactory().createJsonGenerator(os, JsonEncoding.UTF8);
                jg.writeStartArray();

                ArrayList<HashMap> results = new ArrayList<>();
                ArrayList<Node> edgeEmailNodes = new ArrayList<>();

                // Validate our input or exit right away
                HashMap input = getValidQueryInput(body);

                try (Transaction tx = db.beginTx()) {
                    final Node centerNode;
                    try {
                        centerNode = db.getNodeById(emails.get((String) input.get("center_email")));
                    } catch (ExecutionException e) {
                        throw Exceptions.invalidCenterEmailParameter;
                    }
                    for (String edgeEmail : (ArrayList<String>) input.get("edge_emails")) {
                        Long id;
                        try {
                            id = emails.get(edgeEmail);
                        } catch (Exception e) {
                            continue;
                        }
                        final Node edgeEmailNode = db.getNodeById(id);
                        edgeEmailNodes.add(edgeEmailNode);
                    }

                    PathExpander<?> expander = PathExpanders.allTypesAndDirections();
                    PathFinder<org.neo4j.graphdb.Path> shortestPath = GraphAlgoFactory.shortestPath(expander, (int) input.get("length"));

                    for (Node edgeEmail : edgeEmailNodes) {
                        HashMap<String, Object> result = new HashMap<>();
                        int length = 0;
                        int[] count = {0};
                        for (org.neo4j.graphdb.Path path : shortestPath.findAllPaths(centerNode, edgeEmail)) {
                            length = path.length();
                            count[0]++;
                        }
                        jg.writeStartObject();
                        jg.writeStringField("email", (String) edgeEmail.getProperty("email", ""));
                        jg.writeNumberField("length", length);
                        jg.writeNumberField("count", count[0]);
                        jg.writeEndObject();
                    }
                }
                jg.writeEndArray();
                jg.flush();
                jg.close();
            }
        };
        return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * JSON formatted body requires:
     *  center_email: An email address
     *  edge_emails: An Array of email addresses
     *  length: An integer representing the maximum traversal search length
     */
    @POST
    @Path("/query3")
    public Response query3(String body, @Context GraphDatabaseService db) throws IOException, ExecutionException {
        ArrayList<HashMap> results = new ArrayList<>();
        ArrayList<Long> edgeEmailNodeIds = new ArrayList<>();
        ArrayList<Node> edgeEmailNodes = new ArrayList<>();
        SimpleCounter<Long>[] counters = new SimpleCounter[] { new SimpleCounter(), new SimpleCounter(), new SimpleCounter(), new SimpleCounter()};

        // Validate our input or exit right away
        HashMap input = getValidQueryInput(body);

        try (Transaction tx = db.beginTx()) {
            final Node centerNode;
            try {
                centerNode = db.getNodeById(emails.get((String) input.get("center_email")));
            } catch (ExecutionException e) {
                throw Exceptions.invalidCenterEmailParameter;
            }

            for (Relationship rel : centerNode.getRelationships()) {
                counters[0].increment(rel.getOtherNode(centerNode).getId());
            }

            Set<Long> level1 = counters[0].getKeys();
            for (Long id : level1) {
                Node friend = db.getNodeById(id);
                for (Relationship rel : friend.getRelationships()) {
                    counters[1].increment(rel.getOtherNode(friend).getId());
                }
            }

            for (Long id : level1) {
                counters[1].remove(id);
            }

            Set<Long> level2 = counters[1].getKeys();
            for (Long id : level2) {
                Node friend = db.getNodeById(id);
                for (Relationship rel : friend.getRelationships()) {
                    counters[2].increment(rel.getOtherNode(friend).getId());
                }
            }

            for (Long id : level1) {
                counters[2].remove(id);
            }

            for (Long id : level2) {
                counters[2].remove(id);
            }

            Set<Long> level3 = counters[2].getKeys();
            for (Long id : level3) {
                Node friend = db.getNodeById(id);
                for (Relationship rel : friend.getRelationships()) {
                    counters[3].increment(rel.getOtherNode(friend).getId());
                }
            }

            for (Long id : level1) {
                counters[3].remove(id);
            }

            for (Long id : level2) {
                counters[3].remove(id);
            }

            for (Long id : level3) {
                counters[3].remove(id);
            }

            for (String edgeEmail : (ArrayList<String>) input.get("edge_emails")) {
                HashMap<String, Object> result = new HashMap<>();
                Long id;
                try {
                    id = emails.get(edgeEmail);
                } catch (Exception e) {
                    continue;
                }
                int length = 0;
                int count = counters[0].getCount(id);
                if (count > 0) {
                    length = 1;
                } else {
                    count = counters[1].getCount(id);
                    if (count > 0) {
                        length = 2;
                    } else {
                        count = counters[2].getCount(id);
                        if (count > 0) {
                            length = 3;
                        } else {
                            count = counters[3].getCount(id);
                            length = 4;
                        }
                    }
                }

                if ( count > 0 ) {
                    result.put("email", edgeEmail);
                    result.put("length", length);
                    result.put("count", count);
                    results.add(result);
                }


            }
        }
        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

}
