package ixa.srl;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.InputStreamContentProvider;

public class SRLClient {

    private static final int DEFAULT_PORT = 9000;

    public static void main(String[] args) throws Exception {
        CommandLine cmd = parseOptions(args);
        int port = DEFAULT_PORT;
        if (cmd.hasOption("p")) {
            port = Integer.parseInt(cmd.getOptionValue("p"));
        }
        String host = cmd.getOptionValue("h", "localhost");

        HttpClient client = new HttpClient();
        client.start();
        ContentProvider document = new InputStreamContentProvider(System.in);
        String parseUrl = String.format("http://%s:%d?lang=eng", host, port);
        ContentResponse response = client.POST(parseUrl).content(document).send();
        System.out.println(response.getContentAsString());
    }
    
    private static CommandLine parseOptions(String[] args) {
        Options options = new Options();
        options.addOption("p", "port", true, "Port of SRL server");
        options.addOption("h", "host", true, "Host of SRL server");
        try {
            return new PosixParser().parse(options, args);
        } catch (ParseException e) {
            new HelpFormatter().printHelp("server [options]", options);
            System.exit(1);
            return null;
        }
    }

}
