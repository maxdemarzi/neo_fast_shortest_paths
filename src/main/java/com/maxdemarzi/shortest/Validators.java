package com.maxdemarzi.shortest;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;

public class Validators {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static HashMap getValidQueryInput(String body) throws IOException {
        HashMap input;

        // Parse the input
        try {
            input = objectMapper.readValue(body, HashMap.class);
        } catch (Exceptions e) {
            throw Exceptions.invalidInput;
        }
        // Make sure it has a center_email parameter
        if (!input.containsKey("center_email")) {
            throw Exceptions.missingCenterEmailParameter;
        }
        // Make sure it has a edge_emails parameter
        if (!input.containsKey("edge_emails")) {
            throw Exceptions.missingEdgeEmailsParameter;
        }
        // Make sure the length is not blank
        if (input.get("length") == "") {
            throw Exceptions.missingLengthParameter;
        }

        // Make sure the center_email is not blank
        if (input.get("center_email") == "") {
            throw Exceptions.invalidCenterEmailParameter;
        }
        // Make sure the edge_emails is not blank
        if (input.get("edge_emails") == "") {
            throw Exceptions.invalidEdgeEmailsParameter;
        }
        // Make sure the length is not blank
        if (input.get("length") == "") {
            throw Exceptions.invalidLengthParameter;
        }

        return input;
    }

}
