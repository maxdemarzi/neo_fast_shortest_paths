# neo_fast_shortest_paths
faster shortest path request than cypher... hopefully

# Instructions

1. Build it:

        mvn clean package

2. Copy target/faster-shortest-paths-1.0.jar to the plugins/ directory of your Neo4j server.

3. Download and copy additional jar to the plugins/ directory of your Neo4j server. 

        curl -O http://central.maven.org/maven2/com/google/guava/guava/18.0/guava-18.0.jar
        curl -O http://central.maven.org/maven2/net/openhft/koloboke-api-jdk8/0.6.7/koloboke-api-jdk8-0.6.7.jar
        curl -O http://central.maven.org/maven2/net/openhft/koloboke-impl-jdk8/0.6.7/koloboke-impl-jdk8-0.6.7.jar
        
4. Configure Neo4j by adding a line to conf/neo4j-server.properties:

        org.neo4j.server.thirdparty_jaxrs_classes=com.maxdemarzi.shortest=/v1
        
5. Start Neo4j server.

6. Check that it is installed correctly over HTTP:

        :GET /v1/service/helloworld

8. Create test data:
        
        CREATE (start:Email {email:'start@maxdemarzi.com'})
        CREATE (one:Email {email:'one@maxdemarzi.com'})
        CREATE (two:Email {email:'two@maxdemarzi.com'})
        CREATE (three:Email {email:'three@maxdemarzi.com'})
        CREATE (four:Email {email:'four@maxdemarzi.com'})
        CREATE (five:Email {email:'five@maxdemarzi.com'})
        CREATE (nope:Email {email:'unconnected@maxdemarzi.com'})
        CREATE (six:Email {email:'six@maxdemarzi.com'})
        CREATE (seven:Email {email:'seven@maxdemarzi.com'})
        CREATE (eight:Email {email:'eight@maxdemarzi.com'})
        CREATE (start)-[:CONNECTS]->(one)
        CREATE (one)-[:CONNECTS]->(two)
        CREATE (one)-[:CONNECTS]->(three)
        CREATE (one)-[:CONNECTS]->(four)
        CREATE (three)<-[:CONNECTS]-(five)
        CREATE (four)<-[:CONNECTS]-(five)
        CREATE (two)-[:CONNECTS]->(six)
        CREATE (six)-[:CONNECTS]->(seven)
        CREATE (seven)-[:CONNECTS]->(eight)
        
9. Query the database:
        
        :POST /v1/service/query {"center_email":"start@maxdemarzi.com", "edge_emails":["four@maxdemarzi.com","five@maxdemarzi.com"], "length":4}        
        
        curl -H "Content-Type: application/json" -X POST -d '{"center_email":"start@maxdemarzi.com", "edge_emails":["four@maxdemarzi.com","five@maxdemarzi.com"], "length":4}' http://localhost:7474/v1/service/query_counters2