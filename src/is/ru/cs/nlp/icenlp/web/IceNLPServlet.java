package is.ru.cs.nlp.icenlp.web;

import is.iclt.icenlp.core.icemorphy.IceMorphyLexicons;
import is.iclt.icenlp.core.icemorphy.IceMorphyResources;
import is.iclt.icenlp.core.icetagger.IceTagger;
import is.iclt.icenlp.facade.*;
import is.iclt.icenlp.core.tokenizer.*;
import is.iclt.icenlp.core.icetagger.IceTaggerLexicons;
import is.iclt.icenlp.core.icetagger.IceTaggerResources;
import is.iclt.icenlp.core.tritagger.TriTaggerLexicons;
import is.iclt.icenlp.core.tritagger.TriTaggerResources;
import is.iclt.icenlp.core.utils.IceTag;
import is.iclt.icenlp.core.utils.Lexicon;

import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import is.iclt.icenlp.core.lemmald.Lemmald;
import is.iclt.icenlp.core.iceparser.OutputFormatter;


/**
 * Web interface for tagging and parsing Icelandic text
 * @author Sverrir Sigmundarson
 * Various changes made by Hrafn Loftsson
 */
public class IceNLPServlet extends HttpServlet
{
    private String defaultEncoding="UTF-8";
    private Lexicon tokLex=null;
    private IceTaggerLexicons iceLex=null;
    private IceMorphyLexicons morphyLex=null;
    private TriTaggerLexicons triLex=null;
    private IceTaggerFacade itf=null;
    private IceParserFacade ipf=null;
    private Segmentizer segmentizer = null;
    Lemmald lemmald = null;
    OutputFormatter.OutputType outType=OutputFormatter.OutputType.plain;
    

    @Override
    public  void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        //defaultEncoding = FileEncoding.getEncoding();

        TokenizerResources tokResources = new TokenizerResources();
        if (tokResources.isLexicon == null) throw new ServletException( "Could not locate token dictionary");

        IceTaggerResources iceResources = new IceTaggerResources();
        IceMorphyResources morphyResources = new IceMorphyResources();

        if( morphyResources.isDictionaryBase == null ) throw new ServletException("Could not locate base dictionary");
        if( morphyResources.isDictionary == null ) throw new ServletException("Could not locate otb dictionary");
        if( morphyResources.isEndingsBase == null ) throw new ServletException("Could not locate endings base dictionary");
		if( morphyResources.isEndings == null ) throw new ServletException("Could not locate endings dictionary");
		if( morphyResources.isEndingsProper == null ) throw new ServletException("Could not locate endings proper dictionary");
		if( morphyResources.isPrefixes == null ) throw new ServletException("Could not locate prefixes dictionary");
		if( morphyResources.isTagFrequency == null ) throw new ServletException("Could not locate tag frequency dictionary" );
		if( iceResources.isIdioms == null ) throw new ServletException("Could not locate idioms dictionary" );
		if( iceResources.isVerbPrep == null ) throw new ServletException("Could not locate verb prep dictionary" );
		if( iceResources.isVerbObj == null ) throw new ServletException("Could not locate verb obj dictionary");
		if( iceResources.isVerbAdverb == null ) throw new ServletException("Could not locate verb adverb dictionary" );
        
        // For TriTagger
        TriTaggerResources triResources = new TriTaggerResources();
		if( triResources.isNgrams == null ) throw new ServletException("Could not locate model ngram");
		if( triResources.isLambda == null ) throw new ServletException( "Could not locate lambdas");
		if( triResources.isFrequency == null ) throw new ServletException("Could not locate model frequency");

        try
        {
            tokLex = new Lexicon(tokResources.isLexicon);
            this.segmentizer = new Segmentizer(tokLex);
        }
        catch (IOException e) {throw new ServletException("Could not create tokenizer lexicon", e); }

        try {
            morphyLex = new IceMorphyLexicons(morphyResources);
        } catch (IOException e) {throw new ServletException("Could not create IceMorphy lexicon", e); }

        try {
                iceLex = new IceTaggerLexicons(iceResources);
        } catch (IOException e) {throw new ServletException("Could not create IceTagger lexicon", e); }

        try {
            triLex = new TriTaggerLexicons(triResources, true);
        } catch (IOException e) {throw new ServletException("Could not create TriTagger lexicon", e); }


        try {
                itf = new IceTaggerFacade(iceLex, morphyLex, tokLex);
                itf.createTriTagger(triLex);
                ipf = new IceParserFacade();
        }
        catch (IOException e) {throw new ServletException("Could not create Facade objects", e); }

        // Let's get a lemmald instance.
        this.lemmald = Lemmald.getInstance();

    }

    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
	{
		doPost( request, response );
	}

    private void writeTaggedText(Sentences sents, PrintWriter out, boolean sentLine, boolean markUnknown, boolean english, boolean showLemma)
    {
        out.write("<p>");
        for( Sentence sent : sents.getSentences() ) {
            ArrayList tokenList = sent.getTokens();
            for (int i=0; i<=tokenList.size()-1; i++) {
                IceTokenTags tok = (IceTokenTags)tokenList.get(i);
                out.write(tok.lexeme + " ");
                IceTag tag = (IceTag)tok.getFirstTag();
                //out.write("<span title=" + "\"" + tag.annotation(english) + "\"" + ">" + tag + "</span>");
                out.write("<span style=" + "\"" + "color: red" + "\"" + " title=" + "\"" + tag.annotation(english) + "\"" + ">" + tag + "</span> ");
                if(showLemma)
                    out.write("<span style=\"color: blue\">(" + this.lemmald.lemmatize(tok.lexeme,tok.getFirstTagStr()).getLemma()+")</span>");

                if (!sentLine) {
                    if (markUnknown && tok.isUnknown())
                        out.write(" *");
                    out.write("<br>");
                }
                else
                   out.write(" ");
            }
            out.write("<br>");
        }
        out.write("</p>");
    }

    private void tokenize(String query, PrintWriter out, boolean english, boolean useStricktToken, int inputTokenizeType) throws IOException
    {
        if (english)
            out.write("<h3>Tokenisation:</h3>");
        else
            out.write("<h3>Tilreiðing:</h3>");

        Tokenizer tok = new Tokenizer(inputTokenizeType, useStricktToken, this.tokLex);
        segmentizer.segmentize(query);
        while(segmentizer.hasMoreSentences())
        {
           String sentenceStr = segmentizer.getNextSentence();
           tok.tokenize(sentenceStr);
           if(tok.tokens.size() <= 0)
               continue;

           tok.splitAbbreviations();

           for(Object token : tok.tokens)
             out.write("<span>" + ((TokenTags)token).lexeme + "</span><br />");

           out.write("<br />");
       }
    }
    
    @Override
	protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
	{
        boolean functions=false, phraseLine=false, sentLine=false, markUnknown=false;
        boolean /*useHybrid=false,*/ showLemma=false, showTokenization=false, strictTokenization=false;
        boolean mergeLabels=false, featureAgreement=false, showErrors=false;
        IceTagger.HmmModelType modelType = IceTagger.HmmModelType.none;

        // Get the request handles
        request.setCharacterEncoding(defaultEncoding);
        String query = request.getParameter( "query" );
        
        boolean english = (request.getParameter("english").equals("true"));

        if(request.getParameter("functions") != null)
            functions = (request.getParameter("functions").equals("true"));
        if(request.getParameter("phraseline") != null)
            phraseLine = (request.getParameter("phraseline").equals("true"));
        if(request.getParameter("mergelabels") != null)
            mergeLabels = (request.getParameter("mergelabels").equals("true"));
        if(request.getParameter("sentline") != null)
            sentLine = (request.getParameter("sentline").equals("true"));
        if(request.getParameter("markunknown") != null)
            markUnknown = (request.getParameter("markunknown").equals("true"));
        //if(request.getParameter("tagger") != null)
        //    useHybrid = (request.getParameter("tagger").equals("Hybrid"));
        // Set the model type in case using an HMM model
        if(request.getParameter("tagger") != null) {
            if (request.getParameter("tagger").equals("IceTagger"))
                modelType = IceTagger.HmmModelType.none;
            else if (request.getParameter("tagger").equals("HMMIce"))
                modelType = IceTagger.HmmModelType.start;
            else if (request.getParameter("tagger").equals("IceHMM"))
                modelType = IceTagger.HmmModelType.end;
            else if (request.getParameter("tagger").equals("HMMIceHMM"))
                modelType = IceTagger.HmmModelType.startend;

        }
        if(request.getParameter("showlemma") != null)
            showLemma = (request.getParameter("showlemma").equals("true"));
        if(request.getParameter("showerrors") != null)
            showErrors = (request.getParameter("showerrors").equals("true"));
        if(request.getParameter("agreement") != null)
            featureAgreement = (request.getParameter("agreement").equals("true"));


        // Selection of tokenization.
        if(request.getParameter("showTokenize") != null)
            showTokenization = (request.getParameter("showTokenize").equals("true"));
        
        // Selection of the tokenizition type.
        if(request.getParameter("stricktTokenize") != null)
            strictTokenization = (request.getParameter("stricktTokenize").equals("true"));
        int inputTokenizeType = Integer.parseInt(request.getParameter("inputTokenize"));

        // Return the fully tagged and parsed string
        response.setContentType("text/html;charset="+defaultEncoding);
        PrintWriter out = response.getWriter();
        // Tag the query
        analyse(query, out, english, sentLine, markUnknown, functions, phraseLine, mergeLabels, featureAgreement, showErrors, modelType, showTokenization, strictTokenization,inputTokenizeType, showLemma);
	}

    private void testDict()
    {
       Lexicon baseDict = morphyLex.baseDict;

       for (Enumeration keys = baseDict.keys() ; keys.hasMoreElements() ;) {
          System.out.println(keys.nextElement());
       }
    }
    
    private void analyse(String query, PrintWriter out, boolean english, boolean sentLine, boolean markUnknown,
                         boolean functions, boolean phraseLine, boolean mergeLabels, boolean featureAgreement, boolean showErrors,
                         IceTagger.HmmModelType modelType, boolean showTokenization, boolean useStricktToken, int inputTokenizeType, boolean showLemma) throws IOException
    {
        String headColorBegin = "<FONT COLOR=" + "\"" + "blue" +"\"" + ">";
        String headColorEnd = "</FONT>";
        out.write("<html>");
        out.write("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + defaultEncoding + "\"></head>");
        out.write("<body>");
        if (english) {
           out.write("<h2> " + headColorBegin + " IceNLP - A Natural Language Processing Toolkit for Icelandic " + headColorEnd + " </h2>");
           out.write("<h3>Original text:</h3>");
        }
        else {
            out.write("<h2> " + headColorBegin + " IceNLP - Málvinnslutól fyrir íslensku " + headColorEnd + " </h2>");
            out.write("<h3>Upphaflegur texti:</h3>");
        }
        out.write("<p>" + query + "</p>");

        // Only show output of tokenization?
        if (showTokenization)
           tokenize(query, out, english, useStricktToken, inputTokenizeType);
        // else do both tagging and parsing
        else {
            if (english) {
               out.write("<h3>Tagged text (position the mouse over tags to see explanations");
               if (!sentLine && markUnknown)
                  out.write("; unknown words are marked with *");
               out.write("):</h3>");
            }
            else {
                out.write("<h3>Markaður texti (bendið með músinni á mörk til að sjá skýringar");
                if (!sentLine && markUnknown)
                   out.write("; óþekkt orð eru merkt með *");
                out.write("):</h3>");
            }

            // Tag
            long tagStart = System.currentTimeMillis();
            //itf.useTriTagger(useHybrid);
            itf.setModelType(modelType);

            // Debug
            /*if (modelType == IceTagger.HmmModelType.none) out.write("Model: None\n");
            else if (modelType == IceTagger.HmmModelType.start) out.write("Model: Start\n");
            else if (modelType == IceTagger.HmmModelType.end) out.write("Model: End\n");
            else if (modelType == IceTagger.HmmModelType.startend) out.write("Model: StartEnd\n");*/

            Sentences sents = itf.tag(query);
            long tagEnd = System.currentTimeMillis();

            // Parse
            long parseStart = System.currentTimeMillis();
            if (phraseLine)
                outType = OutputFormatter.OutputType.phrase_per_line;
            else
                outType = OutputFormatter.OutputType.plain;

            String parsed = ipf.parse( sents.toString(), outType, functions, featureAgreement, showErrors, mergeLabels );
            long parseEnd = System.currentTimeMillis();

            writeTaggedText(sents, out, sentLine, markUnknown, english, showLemma);

            if (english)
               out.write("<h3>Parsed text:</h3>");
            else
               out.write("<h3>Þáttaður texti:</h3>");
            out.write("<p>" + parsed.replaceAll( "\n", "<br>") + "</p>" );

            if (english) {
               out.write("<h3>Time:</h3>");
               out.write("Tagging: " + (tagEnd - tagStart) + " msec.<br>");
               out.write("Parsing: " + (parseEnd - parseStart) + " msec.<br>" );
               out.write("Total: " + ((tagEnd - tagStart) + (parseEnd - parseStart)) + " msec." );
            }
            else {
               out.write("<h3>Tími:</h3>");
               out.write("Mörkun: " + (tagEnd - tagStart) + " msek.<br>");
               out.write("Þáttun: " + (parseEnd - parseStart) + " msek.<br>" );
               out.write("Heildartími: " + ((tagEnd - tagStart) + (parseEnd - parseStart)) + " msek." );
            }
		}
        out.write( "</body></html>" );
    }

    private boolean printWebError( PrintWriter out, String errorstring )
	{
		out.write( errorstring + "<br />" );
		return true;
	}
}
