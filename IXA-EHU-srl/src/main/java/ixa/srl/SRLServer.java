package ixa.srl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class SRLServer {

    private static final int DEFAULT_PORT = 9000;
    
    private Server httpServer;
    
    public SRLServer(int port) throws Exception {
        httpServer = new Server(port);
        httpServer.setHandler(new Handler());
    }

    public void start() throws Exception {
        httpServer.start();
    }

    public void stop() throws Exception {
        httpServer.stop();
    }

    public void join() throws InterruptedException {
        httpServer.join();
    }

    public static void main(String[] args) throws Exception
    {
        CommandLine cmd = parseOptions(args);
        int port = DEFAULT_PORT;
        if (cmd.hasOption("p")) {
            port = Integer.parseInt(cmd.getOptionValue("p"));
        }
        SRLServer server = new SRLServer(port);
        server.start();
        server.join();
    }
    
    private static CommandLine parseOptions(String[] args) {
        Options options = new Options();
        options.addOption("p", "port", true, "Port to listen to");
        try {
            return new PosixParser().parse(options, args);
        } catch (ParseException e) {
            new HelpFormatter().printHelp("server [options]", options);
            System.exit(1);
            return null;
        }
    }

    public static class Handler extends AbstractHandler
    {
        private SRLService service = new SRLService();
        
        public synchronized void handle(String target, Request baseRequest,
                HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
        {
            try {
                request.setCharacterEncoding("UTF-8");
                response.setContentType("text/xml;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);

                String lang = firstNotNull(request.getParameter("lang"), "eng");
                String option = firstNotNull(request.getParameter("option"), "");
                service.annotate(request.getReader(), response.getWriter(), lang, option);

                baseRequest.setHandled(true);
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }

        private String firstNotNull(String... strings) {
            for (int i = 0; i < strings.length; i++) {
                if (strings[i] != null) {
                    return strings[i];
                }
            }
            return null;
        }
    }
}
