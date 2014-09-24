package ixa.srl;

import ixa.kaflib.KAFDocument;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import se.lth.cs.srl.options.CompletePipelineCMDLineOptions;

public class SRLServer {

    private static final int DEFAULT_PORT = 8080;
    
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
        SRLServer server = new SRLServer(DEFAULT_PORT);
        server.start();
        server.join();
    }
    
    public static class Handler extends AbstractHandler
    {
        private Annotate annotator = new Annotate();
        private String currLang = "";
        private String currOption = "";
        private MatePipeline currPipeline = null;
        
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
                KAFDocument kaf = KAFDocument.createFromStream(request.getReader());
                annotator.SRLToKAF(kaf, lang, option, getMatePipeline(lang, option));
                response.getWriter().write(kaf.toString());

                baseRequest.setHandled(true);
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
        
        private synchronized MatePipeline getMatePipeline(String lang,
                String option) throws Exception {
            if (currPipeline == null ||
                    !currLang.equals(lang) || !currOption.equals(option)) {
                CompletePipelineCMDLineOptions options = 
                        MatePipeline.parseOptions(lang, option);
                currPipeline = MatePipeline.getCompletePipeline(options, option);
                currLang = lang;
                currOption = option;
            }
            return currPipeline;
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
