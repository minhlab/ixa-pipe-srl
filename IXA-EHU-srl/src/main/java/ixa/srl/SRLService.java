package ixa.srl;

import ixa.kaflib.KAFDocument;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import se.lth.cs.srl.options.CompletePipelineCMDLineOptions;

public class SRLService {

    private Annotate annotator = new Annotate();
    private MatePipeline currPipeline = null;
    private String currLang = "";
    private String currOption = "";
    
    public void annotate(Reader reader, Writer writer,
            String lang, String option) throws IOException, Exception {
        KAFDocument kaf = KAFDocument.createFromStream(reader);
        annotator.SRLToKAF(kaf, lang, option, getMatePipeline(lang, option));
        writer.write(kaf.toString());
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

}
