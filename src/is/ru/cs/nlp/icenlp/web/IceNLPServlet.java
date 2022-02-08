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

import javax.servlet.ServletContext;
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

import org.json.*;
import java.nio.charset.StandardCharsets;

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

		// Overwrite the default dictionary with a one with data from BÍN
        ServletContext context = getServletContext();
        InputStream binDict = context.getResourceAsStream( "/WEB-INF/otbBin.dict" );
		morphyResources.setDictionary(binDict);
        
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
/*
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
	{
		doPost( request, response );
	}
	*/

    private void writeTaggedText(Sentences sents, PrintWriter out, boolean sentLine, boolean markUnknown, boolean english, boolean showLemma)
    {
        out.write(",\"texts\":[");
	int j = 0;
        for( Sentence sent : sents.getSentences() ) {
            ArrayList tokenList = sent.getTokens();
	    if (j!=0) out.write(",");
	    j++;
            for (int i=0; i<=tokenList.size()-1; i++) {
		out.write("{");
                IceTokenTags tok = (IceTokenTags)tokenList.get(i);
                out.write("\"content\":\""+tok.lexeme + "\"");
                IceTag tag = (IceTag)tok.getFirstTag();
		out.write(",\"features\":{");
                //out.write("<span title=" + "\"" + tag.annotation(english) + "\"" + ">" + tag + "</span>");
                out.write("\"annotation\":\"" + tag.annotation(english)+"\"");
                out.write(",\"tag\":\"" + tag +"\"");
		/*
                //if(showLemma)
                    //out.write(", \"lemmald\":\"" + this.lemmald.lemmatize(tok.lexeme,tok.getFirstTagStr()).getLemma()+"");

                if (!sentLine) 
                    if (markUnknown && tok.isUnknown())
                	out.write(", \"unknown\": \"True\"");
		*/
		out.write("}}");
		if ( i!=tokenList.size()-1) out.write(",");
            }

        }
        out.write("]");
    }

    private void tokenize(String query, PrintWriter out, boolean english, boolean useStricktToken, int inputTokenizeType) throws IOException
    {

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
             out.write("{" + ((TokenTags)token).lexeme + "}");

       }
    }
    private String errorString(String namespace, String message) {
	String error = "";
	error += "{";
	error += "\"code\":\""+namespace+"\",";
	error += "\"text\":\"Default text to use for the {0} if no {1} can be found\",";
	error += "\"params\":[\"message\", \"translation\"],";
	error += "\"detail\":{";
	error += "\"Error\":\""+message+"\"";
	error += "}";
    	error += "}";
	return error;
    }
    
    @Override
	protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
	{
        boolean functions=false, phraseLine=false, sentLine=false, markUnknown=false;
        boolean /*useHybrid=false,*/ showLemma=false, showTokenization=false, strictTokenization=false;
        boolean mergeLabels=false, featureAgreement=false, showErrors=false;
        IceTagger.HmmModelType modelType = IceTagger.HmmModelType.none;


        // Get the request handles
	response.setContentType("application/json");
	response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

	String jb = new String();
	String line = null;
	try {
		InputStream inputStream = request.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream , StandardCharsets.UTF_8));
		//BufferedReader reader = request.getReader();
		while ((line = reader.readLine()) != null) {
			jb += line;
			String utf8String = new String(line.getBytes(StandardCharsets.UTF_8));
		}

	} catch (Exception e) { /*report an error*/ }
	String str = jb.replace("\\\"","\"");
	JSONObject json_request;
	try {
		json_request = new JSONObject(str);
	}
	catch(org.json.JSONException e) {
		out.write(errorString("iceland.icenlp.no.json", "Input needs to be valid json"));
		out.flush();
		return;
	}

	///*
        //String query = request.getParameter( "query" );
        if(!json_request.has("content")) {
		out.write(errorString("iceland.icenlp.no.query", "'content' was not among the inputs"));
		out.flush();
		return;
	}
        String query = json_request.getString("content");
        
        //boolean english = (request.getParameter("english").equals("true"));
        boolean english = true;
        if(json_request.has("functions")) 
        	functions = (json_request.getString("functions").equals("true"));
        if(json_request.has("phraseline")) 
        	phraseLine = (json_request.getString("phraseline").equals("true"));
        if(json_request.has("mergelabels")) 
        	mergeLabels = (json_request.getString("mergelabels").equals("true"));
        //if(json_request.has("sentline")) 
        	//sentLine = (json_request.getString("sentline").equals("true"));
        if(json_request.has("markunknown")) 
        	markUnknown = (json_request.getString("markunknown").equals("true"));
        if(json_request.has("tagger")) 
            if (json_request.getString("tagger").equals("IceTagger"))
                modelType = IceTagger.HmmModelType.none;
            else if (json_request.getString("tagger").equals("HMMIce"))
                modelType = IceTagger.HmmModelType.start;
            else if (json_request.getString("tagger").equals("IceHMM"))
                modelType = IceTagger.HmmModelType.end;
            else if (json_request.getString("tagger").equals("HMMIceHMM"))
                modelType = IceTagger.HmmModelType.startend;

        //if(json_request.has("showLemma")) 
        	//showLemma = (json_request.getString("showlemma").equals("true"));
        //if(json_request.has("showerrors")) 
        	//showErrors = (json_request.getString("showerrors").equals("true"));
        if(json_request.has("agreement")) 
        	featureAgreement = (json_request.getString("agreement").equals("true"));


        // Selection of tokenization.
        if(json_request.has("showTokenize")) 
		showTokenization = (json_request.getString("showTokenize").equals("true"));
        
        // Selection of the tokenizition type.
        if(json_request.has("stricktTokenize"))
        	strictTokenization = (json_request.getString("stricktTokenize").equals("true"));
	int inputTokenizeType = 0;
        if(json_request.has("inputTokenize")){
        	inputTokenizeType = json_request.getInt("inputTokenize");
	}
        // Return the fully tagged and parsed string
	//response.setContentType("text/html;charset="+defaultEncoding);
	out.write("{");
	out.write("\"response\":{");
	out.write("\"type\":\"texts\"");
        // Tag the query
        analyse(query, out, english, sentLine, markUnknown, functions, phraseLine, mergeLabels, featureAgreement, showErrors, modelType, showTokenization, strictTokenization,inputTokenizeType, showLemma);
	out.write("}");
	out.write("}");
	out.flush();
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

        // Only show output of tokenization?
        if (showTokenization)
           tokenize(query, out, english, useStricktToken, inputTokenizeType);
        // else do both tagging and parsing
        else {

            // Tag
            long tagStart = System.currentTimeMillis();
            //itf.useTriTagger(useHybrid);
            itf.setModelType(modelType);

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

	    //out.write(",\"parsed\":\"" + parsed.replaceAll( "\n", "|")+"\"");

	}
    }

    private boolean printWebError( PrintWriter out, String errorstring )
	{
		out.write( errorstring + "\n" );
		return true;
	}
}
