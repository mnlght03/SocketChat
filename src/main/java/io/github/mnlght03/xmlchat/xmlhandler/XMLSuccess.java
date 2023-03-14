package io.github.mnlght03.xmlchat.xmlhandler;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "success")
public class XMLSuccess {
    private String session;
    private List<XMLUser> userList;

    @XmlElement(name = "session")
    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    @XmlElementWrapper(name = "listusers")
    @XmlElement(name = "user")
    public List<XMLUser> getUserList() {
        return userList;
    }

    public void setUserList(List<XMLUser> userList) {
        this.userList = userList;
    }
}
