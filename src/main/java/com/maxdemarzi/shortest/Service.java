package com.maxdemarzi.shortest;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.*;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
                        public Long load(String email) {
                            return getEmailNodeId(email);
                        }
                    });

    private static Long getEmailNodeId(String email){
        final Node node = db.findNode(Labels.Email, "email", email);
        return node.getId();
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
            final Node centerNode = db.getNodeById(emails.get((String)input.get("center_email")));
            for (String edgeEmail : (ArrayList<String>)input.get("edge_emails")) {
                final Node edgeEmailNode = db.getNodeById(emails.get(edgeEmail));
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
}
