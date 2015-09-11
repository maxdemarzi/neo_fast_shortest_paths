package com.maxdemarzi.shortest;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.openhft.koloboke.collect.LongCursor;
import net.openhft.koloboke.collect.map.hash.HashLongIntMap;
import net.openhft.koloboke.collect.map.hash.HashLongIntMaps;
import net.openhft.koloboke.collect.set.LongSet;
import net.openhft.koloboke.collect.set.hash.HashLongSets;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.cursor.Cursor;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.cursor.NodeItem;
import org.neo4j.kernel.api.cursor.RelationshipItem;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;

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
    private final GraphDatabaseAPI dbAPI;

    public Service(@Context GraphDatabaseService graphDatabaseService) {
        db = graphDatabaseService;
        dbAPI = (GraphDatabaseAPI) db;
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
                return Response.ok().entity("[]").build();
            }

            for (String edgeEmail : (ArrayList<String>)input.get("edge_emails")) {
                Long edgeId;
                try {
                    edgeId = emails.get(edgeEmail);
                } catch (Exception e) {
                    continue;
                }
                final Node edgeEmailNode = db.getNodeById(edgeId);
                edgeEmailNodes.add(edgeEmailNode);
            }

            PathExpander<?> expander =  PathExpanders.allTypesAndDirections();
            PathFinder<org.neo4j.graphdb.Path> shortestPath = GraphAlgoFactory.shortestPath(expander, (int)input.get("length"));

            for (Node edgeEmail : edgeEmailNodes ) {
                HashMap<String, Object> result = new HashMap<>();
                int length = 0;
                int count = 0;
                for ( org.neo4j.graphdb.Path path : shortestPath.findAllPaths( centerNode, edgeEmail ) )
                {
                    length = path.length();
                    count++;
                }

                if (length > 0 && count > 0) {
                  result.put("email", edgeEmail.getProperty("email", ""));
                  result.put("length", length);
                  result.put("count", count);

                  results.add(result);
                }
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
    @Path("/query_streaming")
    public Response query_streaming(String body, @Context GraphDatabaseService db) throws IOException, ExecutionException {

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                JsonGenerator jg = objectMapper.getJsonFactory().createJsonGenerator(os, JsonEncoding.UTF8);

                ArrayList<Node> edgeEmailNodes = new ArrayList<>();

                // Validate our input or exit right away
                HashMap input = getValidQueryInput(body);

                try (Transaction tx = db.beginTx()) {
                    final Node centerNode;
                    try {
                        centerNode = db.getNodeById(emails.get((String) input.get("center_email")));
                    } catch (ExecutionException e) {
                        jg.close();
                        return;
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
                        int count = 0;
                        for ( org.neo4j.graphdb.Path path : shortestPath.findAllPaths( centerNode, edgeEmail ) )
                        {
                            length = path.length();
                            count++;
                        }

                        if (length > 0 && count > 0) {
                            jg.writeStartObject();
                            jg.writeStringField("email", (String) edgeEmail.getProperty("email", ""));
                            jg.writeNumberField("length", length);
                            jg.writeNumberField("count", count);
                            jg.writeEndObject();
                            jg.writeRaw("\n");
                            jg.flush();
                        }
                    }
                }
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
    @Path("/query_counters")
    public Response query_counters(String body, @Context GraphDatabaseService db) throws IOException, ExecutionException {

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                JsonGenerator jg = objectMapper.getJsonFactory().createJsonGenerator(os, JsonEncoding.UTF8);

                // Validate our input or exit right away
                HashMap input = getValidQueryInput(body);
                int maxLength = (int) input.get("length");
                
                try (Transaction tx = db.beginTx()) {
                    final Long centerNodeId;
                    try {
                        centerNodeId = emails.get((String) input.get("center_email"));
                    } catch (ExecutionException e) {
                        jg.close();
                        return;
                    }

                    final Map<Long,String> edgeEmailsByNodeId = new HashMap<>();
                    for (String edgeEmail : (ArrayList<String>) input.get("edge_emails")) {
                        try {
                            edgeEmailsByNodeId.put(emails.get(edgeEmail), edgeEmail);
                        } catch (Exception e) {
                            continue;
                        }
                    }

                    int level = 1;
                    Set<Long> idsForLevel = new HashSet<>();
                    idsForLevel.add(centerNodeId);

                    while (level <= maxLength && !edgeEmailsByNodeId.isEmpty()) {
                        // Get nodes at next level, counting by number of times they appear
                        SimpleCounter<Long> counter = new SimpleCounter();
                        for (Long id : idsForLevel) {
                            Node friend = db.getNodeById(id);
                            for (Relationship rel : friend.getRelationships()) {
                                counter.increment(rel.getOtherNode(friend).getId());
                            }
                        }

                        // Now next level is current level; report any target nodes that appear, then stop searching for them
                        idsForLevel = counter.getKeys();
                        for (Long id : idsForLevel) {
                            if (edgeEmailsByNodeId.containsKey(id)) {
                                jg.writeStartObject();
                                jg.writeStringField("email", edgeEmailsByNodeId.get(id));
                                jg.writeNumberField("length", level);
                                jg.writeNumberField("count", counter.getCount(id));
                                jg.writeEndObject();
                                jg.writeRaw("\n");

                                edgeEmailsByNodeId.remove(id);
                            }
                        }
                        jg.flush();

                        level++;
                    }
                }
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
    @Path("/query_counters2")
    public Response query_counters2(String body, @Context GraphDatabaseService db) throws IOException, ExecutionException {

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                JsonGenerator jg = objectMapper.getJsonFactory().createJsonGenerator(os, JsonEncoding.UTF8);

                // Validate our input or exit right away
                HashMap input = getValidQueryInput(body);
                int maxLength = (int) input.get("length");

                try (Transaction tx = db.beginTx()) {
                    final Long centerNodeId;
                    try {
                        centerNodeId = emails.get((String) input.get("center_email"));
                    } catch (ExecutionException e) {
                        jg.close();
                        return;
                    }

                    final Map<Long,String> edgeEmailsByNodeId = new HashMap<>();
                    for (String edgeEmail : (ArrayList<String>) input.get("edge_emails")) {
                        try {
                            edgeEmailsByNodeId.put(emails.get(edgeEmail), edgeEmail);
                        } catch (Exception e) {
                            continue;
                        }
                    }

                    int level = 1;
                    LongSet idsForlevel = HashLongSets.newMutableSet();
                    idsForlevel.add((long)centerNodeId);

                    while (level <= maxLength && !edgeEmailsByNodeId.isEmpty()) {
                        // Get nodes at next level, counting by number of times they appear
                        HashLongIntMap counter = HashLongIntMaps.newMutableMap();

                        LongCursor longCursor = idsForlevel.cursor();
                        while (longCursor.moveNext()) {
                            Node friend = db.getNodeById(longCursor.elem());
                            for (Relationship rel : friend.getRelationships()) {
                                counter.addValue(rel.getOtherNode(friend).getId(), 1, 0);
                            }
                        }

                        // Now next level is current level; report any target nodes that appear, then stop searching for them
                        idsForlevel = counter.keySet();
                        LongCursor longCursor2 = idsForlevel.cursor();
                        while (longCursor2.moveNext()) {
                            if (edgeEmailsByNodeId.containsKey(longCursor2.elem())) {
                                jg.writeStartObject();
                                jg.writeStringField("email", edgeEmailsByNodeId.get(longCursor2.elem()));
                                jg.writeNumberField("length", level);
                                jg.writeNumberField("count", counter.get(longCursor2.elem()));
                                jg.writeEndObject();
                                jg.writeRaw("\n");

                                edgeEmailsByNodeId.remove(longCursor2.elem());
                            }
                        }
                        jg.flush();

                        level++;
                    }
                }
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
    @Path("/query_counters3")
    public Response query_counters3(String body, @Context GraphDatabaseService db) throws IOException, ExecutionException {

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                JsonGenerator jg = objectMapper.getJsonFactory().createJsonGenerator(os, JsonEncoding.UTF8);

                // Validate our input or exit right away
                HashMap input = getValidQueryInput(body);
                int maxLength = (int) input.get("length");

                try (Transaction tx = db.beginTx()) {
                    ThreadToStatementContextBridge ctx = dbAPI.getDependencyResolver().resolveDependency(ThreadToStatementContextBridge.class);
                    ReadOperations ops = ctx.get().readOperations();

                    final Long centerNodeId;
                    try {
                        centerNodeId = emails.get((String) input.get("center_email"));
                    } catch (ExecutionException e) {
                        jg.close();
                        return;
                    }

                    final Map<Long,String> edgeEmailsByNodeId = new HashMap<>();
                    for (String edgeEmail : (ArrayList<String>) input.get("edge_emails")) {
                        try {
                            edgeEmailsByNodeId.put(emails.get(edgeEmail), edgeEmail);
                        } catch (Exception e) {
                            continue;
                        }
                    }

                    int level = 1;
                    LongSet idsForlevel = HashLongSets.newMutableSet();
                    idsForlevel.add((long) centerNodeId);
                    Cursor<NodeItem> nodeCursor;
                    Cursor<RelationshipItem> relationshipCursor;
                    LongCursor longCursor;

                    while (level <= maxLength && !edgeEmailsByNodeId.isEmpty()) {
                        // Get nodes at next level, counting by number of times they appear
                        HashLongIntMap counter = HashLongIntMaps.newMutableMap();
                        longCursor = idsForlevel.cursor();
                        while (longCursor.moveNext()) {
                            nodeCursor = ops.nodeCursor(longCursor.elem());
                            nodeCursor.next();
                            relationshipCursor = nodeCursor.get().relationships(Direction.BOTH);

                            while (relationshipCursor.next()) {
                                counter.addValue(relationshipCursor.get().otherNode(longCursor.elem()), 1, 0);
                            }
                        }

                        // Now next level is current level; report any target nodes that appear, then stop searching for them
                        idsForlevel = counter.keySet();
                        longCursor = idsForlevel.cursor();

                        while (longCursor.moveNext()) {
                            if (edgeEmailsByNodeId.containsKey(longCursor.elem())) {
                                jg.writeStartObject();
                                jg.writeStringField("email", edgeEmailsByNodeId.get(longCursor.elem()));
                                jg.writeNumberField("length", level);
                                jg.writeNumberField("count", counter.get(longCursor.elem()));
                                jg.writeEndObject();
                                jg.writeRaw("\n");

                                edgeEmailsByNodeId.remove(longCursor.elem());
                            }
                        }
                        jg.flush();

                        level++;
                    }
                }
                jg.close();
            }
        };
        return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();
    }

}
