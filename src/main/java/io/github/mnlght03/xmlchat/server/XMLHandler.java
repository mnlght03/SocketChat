package io.github.mnlght03.xmlchat.server;

import io.github.mnlght03.xmlchat.xmlserializer.XMLError;
import io.github.mnlght03.xmlchat.xmlserializer.XMLEvent;
import io.github.mnlght03.xmlchat.xmlserializer.XMLSuccess;
import io.github.mnlght03.xmlchat.xmlserializer.XMLUser;

import java.util.List;

public class XMLHandler {
    public static XMLEvent createEventMessage(String name, String username, String message) {
        XMLEvent event = new XMLEvent();

        if (name != null) event.setName(name);

        if (username != null) event.setUsername(username);

        if (message != null) event.setMessage(message);

        return event;
    }

    public static XMLSuccess createSuccessMessage(String session, List<XMLUser> userList) {
        XMLSuccess success = new XMLSuccess();

        if (session != null) success.setSession(session);

        if (userList != null) success.setUserList(userList);

        return success;
    }

    public static XMLError createErrorMessage(String message) {
        XMLError error = new XMLError();

        if (message != null) error.setMessage(message);

        return error;
    }
}
