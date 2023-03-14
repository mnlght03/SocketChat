package io.github.mnlght03.xmlchat.xmlhandler;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class XMLSerializer {
    public static Object deserialize(Object obj, String xml) throws IOException, JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(obj.getClass());
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new StringReader(xml));
    }

    public static String serialize(Object obj) throws IOException, JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(obj.getClass());
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(obj, writer);
        return writer.toString();
    }

    public static void main(String[] args) throws IOException, JAXBException {
//        Command command = new Command();
//        command.setMessage("qweasd");
//        command.setName("Qwerty");
//        command.setSession(1);
//        String xml = XMLChatHandler.serialize(command);
//        System.out.println(xml);

        String xml = Files.readString(Paths.get("C:/Code/java/Socket_Chat", "src/main/java/io/github/mnlght03/xmlchat",
                "xmlhandler/success.xml"));
        System.out.println(xml);
        Object obj = XMLSerializer.deserialize(new XMLSuccess(), xml);
        System.out.println(obj);
        String xmlSerialized = XMLSerializer.serialize(obj);
        System.out.println(xmlSerialized);
    }
}
