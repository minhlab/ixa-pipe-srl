package ixa.srl;

import java.nio.charset.StandardCharsets;

import junit.framework.TestCase;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;

public class SRLServerTest extends TestCase {

    private SRLServer server;
    private HttpClient client;

    @Override
    protected void setUp() throws Exception {
        server = new SRLServer(8080);
        server.start();
        client = new HttpClient();
        client.start();
    }

    @Override
    protected void tearDown() throws Exception {
        server.stop();
    }

    public void testEmpty() throws Exception {
        ContentProvider emptyContent = new StringContentProvider(
                "<NAF version=\"v3\" xml:lang=\"en\"></NAF>", 
                StandardCharsets.UTF_8);
        ContentResponse response = client.POST("http://localhost:8080?lang=eng")
                .content(emptyContent).send();
        response.getContentAsString();
        assertEquals(200, response.getStatus());
    }

    public void testRealDocument() throws Exception {
        ContentProvider document = new InputStreamContentProvider(
                SRLServerTest.class.getResourceAsStream("/sample-01.naf"));
        ContentResponse response = client.POST("http://localhost:8080?lang=eng")
                .content(document).send();
        System.out.println(response.getContentAsString());
        assertEquals(200, response.getStatus());
    }

    public void testManyDocuments() throws Exception {
        ContentProvider document = new InputStreamContentProvider(
                SRLServerTest.class.getResourceAsStream("/sample-01.naf"));
        // let the server init first
        client.POST("http://localhost:8080?lang=eng").content(document).send();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            ContentResponse response = client
                    .POST("http://localhost:8080?lang=eng").content(document)
                    .send();
            assertEquals(200, response.getStatus());
        }
        long stop = System.currentTimeMillis();
        assertTrue("Processing takes too long", stop-start < 1000);
    }

}
