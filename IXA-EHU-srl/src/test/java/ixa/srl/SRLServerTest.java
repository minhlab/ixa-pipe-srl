package ixa.srl;

import junit.framework.TestCase;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.InputStreamContentProvider;

public class SRLServerTest extends TestCase {

    private static final int PORT = 9000;
    private static final String PARSE_URL = "http://localhost:" + PORT + "?lang=eng";
    
    private SRLServer server;
    private HttpClient client;

    @Override
    protected void setUp() throws Exception {
        server = new SRLServer(PORT);
        server.start();
        client = new HttpClient();
        client.start();
    }

    @Override
    protected void tearDown() throws Exception {
        server.stop();
    }

//    public void testEmpty() throws Exception {
//        ContentProvider emptyContent = new StringContentProvider(
//                "<NAF version=\"v3\" xml:lang=\"en\"></NAF>", 
//                StandardCharsets.UTF_8);
//        ContentResponse response = client.POST(PARSE_URL).content(emptyContent).send();
//        response.getContentAsString();
//        assertEquals(200, response.getStatus());
//    }
//
//    public void testRealDocument() throws Exception {
//        ContentProvider document = new InputStreamContentProvider(
//                SRLServerTest.class.getResourceAsStream("/sample-01.naf"));
//        ContentResponse response = client.POST(PARSE_URL).content(document).send();
//        System.out.println(response.getContentAsString());
//        assertEquals(200, response.getStatus());
//    }

    public void testManyDocuments() throws Exception {
        ContentProvider document = new InputStreamContentProvider(
                SRLServerTest.class.getResourceAsStream("/sample-01.naf"));
        // let the server init first
        client.POST(PARSE_URL).content(document).send();
        long start = System.currentTimeMillis();
        int times = 10;
        for (int i = 0; i < times; i++) {
            ContentResponse response = client.POST(PARSE_URL).content(document).send();
            assertEquals(200, response.getStatus());
        }
        long stop = System.currentTimeMillis();
        double ellapsedSecs = (stop-start)/1000.0;
        System.out.format("Processed %d documents in %f secs.\n", times, ellapsedSecs);
        assertTrue("Processing takes too long", ellapsedSecs < 10);
    }

}
