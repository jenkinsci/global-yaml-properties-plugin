package io.jenkins.plugins.globalyamlproperties;

import com.cloudbees.plugins.credentials.*;
import hudson.security.ACL;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Utils {
    static String readInputStream(InputStream inputStream) throws IOException {
        StringBuilder textBuilder = new StringBuilder();
        Reader reader = new BufferedReader(new InputStreamReader
                (inputStream, StandardCharsets.UTF_8));
        int c = 0;
        while ((c = reader.read()) != -1) {
            textBuilder.append((char) c);
        }
        return textBuilder.toString();
    }

    public static Map<String, Object> deepCopyMap(Map<String, Object> orig) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(orig);
        oos.flush();
        ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bin);
        return (Map<String, Object>) ois.readObject();
    }

    static Credentials getCredentialsById(String credentialsId) {
        List<Credentials> credentialsList = CredentialsProvider.lookupCredentialsInItem(
                Credentials.class,
                null,
                ACL.SYSTEM2,
                Collections.emptyList()
        );
        Credentials requestedCredentials = CredentialsMatchers.firstOrNull(
                credentialsList,
                CredentialsMatchers.withId(credentialsId)
        );
        if (requestedCredentials == null) {
            throw new CredentialsUnavailableException("No credentials found with ID: " + credentialsId);
        }
        return requestedCredentials;

    }
}
