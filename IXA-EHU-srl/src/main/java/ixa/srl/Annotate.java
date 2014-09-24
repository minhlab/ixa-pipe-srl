package ixa.srl;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.regex.Pattern;

import ixa.kaflib.Dep;
import ixa.kaflib.ExternalRef;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Predicate;
import ixa.kaflib.Predicate.Role;
import ixa.kaflib.Span;
import ixa.kaflib.WF;
import ixa.kaflib.Term;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import se.lth.cs.srl.options.CompletePipelineCMDLineOptions;

public class Annotate {

	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	private HashMap<String, String> mapp = new HashMap<String, String>();
	private HashMap<String, String> WFtoken = new HashMap<String, String>();

	private String kaflang = new String();

	MatePipeline mate;

	public Annotate() {
		mate = new MatePipeline();
	}

    public void SRLToKAF(KAFDocument kaf, String lang, String option)
            throws Exception {
        SRLToKAF(kaf, lang, option, null);
    }

    public void SRLToKAF(KAFDocument kaf, String lang, String option, MatePipeline pipeline)
            throws Exception {

		if ("eng".equals(lang)) {
			this.kaflang = "en";
		} else if ("spa".equals(lang)) {
			this.kaflang = "es";
		}

   	    KAFDocument.LinguisticProcessor depsLP = null;
		KAFDocument.LinguisticProcessor srlLP = null;

		if (!"only-srl".equals(option)) {
		    depsLP = kaf.addLinguisticProcessor("deps", "ixa-pipe-srl-" + kaflang, "1.0");
		    depsLP.setBeginTimestamp();
		}
		if (!"only-deps".equals(option)) {
		    srlLP = kaf.addLinguisticProcessor("srl", "ixa-pipe-srl-" + kaflang, "1.0");
		    srlLP.setBeginTimestamp();
		}

		PrintStream printStreamOriginal = System.out;
		System.setOut(new PrintStream(new OutputStream() {
			public void write(int b) {
			}
		}));

		HashMap<String, String> annotationlines = KAF2MateTerms(kaf);
		if ("only-srl".equals(option)) {
			annotationlines = KAF2MateDeps(annotationlines, kaf, kaflang);
		}
		List<String> annotation = KAF2Mate(annotationlines, kaf);

		CompletePipelineCMDLineOptions options = 
		        MatePipeline.parseOptions(lang, option);
		if (pipeline == null) {
		    pipeline = MatePipeline.getCompletePipeline(options, option);
        }
        Document response = MatePipeline.parseCoNLL09(options, option,
                pipeline, annotation);

		System.setOut(printStreamOriginal);

		if (!"only-srl".equals(option)) {
			XMLMate2KAFDEPS(kaf, response);
			depsLP.setEndTimestamp();
		}
		if (!"only-deps".equals(option)) {
			XMLMate2KAFSRL(kaf, response);
			srlLP.setEndTimestamp();
		}
	}

	/*
	 * ID FORM LEMMA PLEMMA POS PPOS FEAT PFEAT HEAD PHEAD DEPREL PDEPREL
	 * FILLPRED PRED APRED
	 */

	private List<String> KAF2Mate(HashMap<String, String> annotationlines,
			KAFDocument kaf) {
		List<String> annotation = new ArrayList<String>();

		int prevSentence = -1;
		List<WF> wfs = kaf.getWFs();
		for (WF wf : wfs) {
			int sentence = wf.getSent();
			if (kaf.getSentences().get(sentence - 1).size() < 100) {
				if (sentence != prevSentence && prevSentence != -1) {
					annotation.add(System.getProperty("line.separator"));
				}
				annotation.add(annotationlines.get(wf.getId()));
				prevSentence = sentence;
			}
		}
		annotation.add(System.getProperty("line.separator"));

		return annotation;
	}

	private HashMap<String, String> KAF2MateTerms(KAFDocument kaf) {

		HashMap<String, String> annotationlines = new HashMap<String, String>();

		int prevSentence = -1;
		int mateSentence = 1;
		int token = 1;
		List<Term> terms = kaf.getTerms();
		for (Term term : terms) {
			String text = "";
			int sentence = term.getSent();
			if (kaf.getSentences().get(sentence - 1).size() < 100) {
				if (sentence != prevSentence && prevSentence != -1) {
					token = 1;
					mateSentence++;
				}
				String lemma = term.getLemma();
				String pos = term.getMorphofeat();
				if (this.kaflang.equals("es")) {
					if (!pos.equals("sn")) {
						pos = pos.substring(0, 1);
						pos = pos.toLowerCase();
					}
				}
				List<WF> wordforms = term.getWFs();
				for (WF wordform : wordforms) {
					text += token + "\t" + wordform.getForm();
					text += "\t" + lemma + "\t" + lemma;
					text += "\t" + pos + "\t" + pos;
					text += "\t_\t_";
					annotationlines.put(wordform.getId(), text);
					mapp.put(mateSentence + " " + (token), wordform.getId());
					WFtoken.put(wordform.getId(), Integer.toString(token));
					token++;
				}
				prevSentence = sentence;
			}
		}
		return annotationlines;
	}

	private HashMap<String, String> KAF2MateDeps(
			HashMap<String, String> annotationlines, KAFDocument kaf,
			String lang) {

		String root = new String();
		if (lang.equals("en")) {
			root = "ROOT";
		} else if (lang.equals("es")) {
			root = "sentence";
		}

		List<Dep> deps = kaf.getDeps();
		for (Dep dep : deps) {
			Term term = dep.getTo();
			Term headterm = dep.getFrom();
			String head;

			if (headterm.getSpan().hasHead()) {
				head = WFtoken.get(headterm.getSpan().getHead().getId());
			} else {
				head = WFtoken.get(headterm.getSpan().getFirstTarget().getId());
			}

			String deprel = dep.getRfunc();

			List<WF> wordforms = term.getWFs();
			for (WF wordform : wordforms) {
				int sentence = wordform.getSent();
				if (kaf.getSentences().get(sentence - 1).size() < 100) {
					String text = annotationlines.get(wordform.getId());
					text += "\t" + head + "\t" + head;
					text += "\t" + deprel + "\t" + deprel;
					annotationlines.put(wordform.getId(), text);
				}
			}
		}

		List<WF> wordforms = kaf.getWFs();
		for (WF wordform : wordforms) {
			int sentence = wordform.getSent();
			if (kaf.getSentences().get(sentence - 1).size() < 100) {
				String text = annotationlines.get(wordform.getId());
				String[] tokens = WHITESPACE_PATTERN.split(text);
				if (tokens.length != 12) {
					text += "\t0\t0";
					text += "\t" + root + "\t" + root;
					annotationlines.put(wordform.getId(), text);
				}
			}
		}
		return annotationlines;
	}

	private void XMLMate2KAFDEPS(KAFDocument kaf, Document doc) {
		NodeList dList = doc.getElementsByTagName("DEP");

		for (int dcont = 0; dcont < dList.getLength(); dcont++) {
			Node dNode = dList.item(dcont);
			if (dNode.getNodeType() == Node.ELEMENT_NODE) {
				Element dElement = (Element) dNode;
				String deptoken = mapp.get(dElement.getAttribute("sentidx")
						+ " " + dElement.getAttribute("toidx"));

				if (!dElement.getAttribute("fromidx").equals("0")) {
					String dephead = mapp.get(dElement.getAttribute("sentidx")
							+ " " + dElement.getAttribute("fromidx"));
					String deprel = dElement.getAttribute("rfunc");
					kaf.newDep(getTerm(kaf, dephead), getTerm(kaf, deptoken),
							deprel);
				}
			}
		}
	}

	private void XMLMate2KAFSRL(KAFDocument kaf, Document doc) {

		PredicateMatrix PM = new PredicateMatrix();
		NodeList pList = doc.getElementsByTagName("PRED");

		for (int pcont = 0; pcont < pList.getLength(); pcont++) {
			Node pNode = pList.item(pcont);
			if (pNode.getNodeType() == Node.ELEMENT_NODE) {
				Element pElement = (Element) pNode;
				String predtoken = mapp.get(pElement.getAttribute("sentidx")
						+ " " + pElement.getAttribute("predidx"));

				List<Term> predterms = new ArrayList<Term>();
				predterms.add(getTerm(kaf, predtoken));
				Span<Term> predSpan = KAFDocument.newTermSpan(predterms);
				Predicate newPred = kaf.newPredicate(predSpan);

				ExternalRef predicateSense;
				if (this.kaflang.equals("es")) {
					predicateSense = kaf.newExternalRef("AnCora",
							pElement.getAttribute("sense"));
				} else {
					if (getTerm(kaf, predtoken).getPos().equals("N"))
						predicateSense = kaf.newExternalRef("NomBank",
								pElement.getAttribute("sense"));
					else
						predicateSense = kaf.newExternalRef("PropBank",
								pElement.getAttribute("sense"));
				}
				newPred.addExternalRef(predicateSense);

				ArrayList<String> vnClasses = PM.getVNClasses(pElement
						.getAttribute("sense"));
				if (!vnClasses.isEmpty())
					for (int vnc = 0; vnc < vnClasses.size(); vnc++) {
						ExternalRef vnClass = kaf.newExternalRef("VerbNet",
								vnClasses.get(vnc));
						newPred.addExternalRef(vnClass);
					}
				ArrayList<String> vnSubClasses = PM.getVNSubClasses(pElement
						.getAttribute("sense"));
				if (!vnSubClasses.isEmpty())
					for (int vnsc = 0; vnsc < vnSubClasses.size(); vnsc++) {
						ExternalRef vnSubClass = kaf.newExternalRef("VerbNet",
								vnSubClasses.get(vnsc));
						newPred.addExternalRef(vnSubClass);
					}
				ArrayList<String> fnFrames = PM.getFNFrames(pElement
						.getAttribute("sense"));
				if (!fnFrames.isEmpty())
					for (int fnf = 0; fnf < fnFrames.size(); fnf++) {
						ExternalRef fnFrame = kaf.newExternalRef("FrameNet",
								fnFrames.get(fnf));
						newPred.addExternalRef(fnFrame);
					}
				ArrayList<String> pbPredicates = PM.getPBPredicates(pElement
						.getAttribute("sense"));
				if (!pbPredicates.isEmpty())
					for (int pbp = 0; pbp < pbPredicates.size(); pbp++) {
						ExternalRef pbPredicate = kaf.newExternalRef("PropBank",
								pbPredicates.get(pbp));
						newPred.addExternalRef(pbPredicate);
					}
				ArrayList<String> eventTypes = PM.getEventTypes(pElement
						.getAttribute("sense"));
				if (!eventTypes.isEmpty())
					for (int et = 0; et < eventTypes.size(); et++) {
						ExternalRef eventType = kaf.newExternalRef("EventType",
								eventTypes.get(et));
						newPred.addExternalRef(eventType);
					}

				NodeList aList = pElement.getElementsByTagName("ARG");
				for (int acont = 0; acont < aList.getLength(); acont++) {
					Node aNode = aList.item(acont);
					if (aNode.getNodeType() == Node.ELEMENT_NODE) {
						Element aElement = (Element) aNode;
						String fillertoken = mapp.get(pElement
								.getAttribute("sentidx")
								+ " "
								+ aElement.getAttribute("filleridx"));

						List<Term> fillerterms = new ArrayList<Term>();
						fillerterms.add(getTerm(kaf, fillertoken));
						fillerterms = getAllChilds(kaf, fillerterms,
								getTerm(kaf, fillertoken));
						Span<Term> fillerSpan = KAFDocument.newTermSpan(
								sortTerms(kaf, fillerterms),
								getTerm(kaf, fillertoken));
						Role newRole = kaf.newRole(newPred,
								aElement.getAttribute("argument"), fillerSpan);

						ArrayList<String> vnThematicRoles = PM
								.getVNThematicRoles(pElement
										.getAttribute("sense")
										+ ":"
										+ aElement.getAttribute("argument"));
						if (!vnThematicRoles.isEmpty())
							for (int vntr = 0; vntr < vnThematicRoles.size(); vntr++) {
								ExternalRef vnThematicRole = kaf
										.newExternalRef("VerbNet",
												vnThematicRoles.get(vntr));
								newRole.addExternalRef(vnThematicRole);
							}
						ArrayList<String> fnFrameElements = PM
								.getFNFrameElements(pElement
										.getAttribute("sense")
										+ ":"
										+ aElement.getAttribute("argument"));
						if (!fnFrameElements.isEmpty())
							for (int fnfe = 0; fnfe < fnFrameElements.size(); fnfe++) {
								ExternalRef fnFrameElement = kaf
										.newExternalRef("FrameNet",
												fnFrameElements.get(fnfe));
								newRole.addExternalRef(fnFrameElement);
							}
						ArrayList<String> pbArguments = PM
								.getPBArguments(pElement
										.getAttribute("sense")
										+ ":"
										+ aElement.getAttribute("argument"));
						if (!pbArguments.isEmpty())
							for (int pba = 0; pba < pbArguments.size(); pba++) {
								ExternalRef pbArgument = kaf
										.newExternalRef("PropBank",
												pbArguments.get(pba));
								newRole.addExternalRef(pbArgument);
							}


						newPred.addRole(newRole);
					}
				}
			}
		}
	}

	private Term getTerm(KAFDocument kaf, String idx) {
		Term termidx = null;
		List<Term> terms = kaf.getTerms();
		for (Term term : terms) {
			List<WF> termWFs = term.getWFs();
			for (WF wf : termWFs) {
				if (wf.getId().equals(idx)) {
					termidx = term;
				}
			}
		}
		return termidx;
	}

	private List<Term> getAllChilds(KAFDocument kaf, List<Term> listChilds,
			Term term) {

		List<Dep> deps = kaf.getDeps();
		for (Dep dep : deps) {
			Term headterm = dep.getFrom();
			if (headterm.getId().equals(term.getId())) {
				Term childterm = dep.getTo();
				// the check is needed to avoid stack overflow error when 
				// there is an erroneous cyclic dependence between nodes
				if (!listChilds.contains(childterm)) {
				    listChilds.add(childterm);
				    getAllChilds(kaf, listChilds, childterm);
                }
			}
		}

		return listChilds;
	}

	private List<Term> sortTerms(KAFDocument kaf, List<Term> listTerms) {
		List<Term> sortedList = new ArrayList<Term>();
		List<Term> terms = kaf.getTerms();
		for (Term term : terms) {
			boolean isInList = false;
			for (Term termInList : listTerms)
				if (term.equals(termInList))
					isInList = true;
			if (isInList)
				sortedList.add(term);
		}

		return sortedList;
	}
}
