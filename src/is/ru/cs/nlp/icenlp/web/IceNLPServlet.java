package is.ru.cs.nlp.icenlp.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.elg.model.Failure;
import eu.elg.model.Markup;
import eu.elg.model.StandardMessages;
import eu.elg.model.requests.TextRequest;
import eu.elg.model.responses.TextsResponse;
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

import java.nio.charset.StandardCharsets;
import java.util.List;

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

    private ObjectMapper mapper = new ObjectMapper();
    

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

    private List<TextsResponse.Text> writeTaggedText(Sentences sents, boolean sentLine, boolean markUnknown, boolean english, boolean showLemma)
    {
      List<TextsResponse.Text> result = new ArrayList<>();
	int j = 0;
        for( Sentence sent : sents.getSentences() ) {
          List<TextsResponse.Text> tokensInSentence = new ArrayList<>();
            ArrayList tokenList = sent.getTokens();
	    j++;
            for (int i=0; i<=tokenList.size()-1; i++) {
                IceTokenTags tok = (IceTokenTags)tokenList.get(i);
                TextsResponse.Text thisTok = new TextsResponse.Text().withContent(tok.lexeme);
                IceTag tag = (IceTag)tok.getFirstTag();
                Markup markup = new Markup()
                        .withFeature("annotation", tag.annotation(english))
                        .withFeature("tag", tag.toString())
                        .withFeature("lemma", this.lemmald.lemmatize(tok.lexeme,tok.getFirstTagStr()).getLemma());

	    	if (tok.isUnknown())
		  markup.withFeature("unknown", "True");
              thisTok.setMarkup(markup);
              tokensInSentence.add(thisTok);
            }
            result.add(new TextsResponse.Text().withTexts(tokensInSentence).withRole("sentence"));
        }
        return result;
    }

    private List<TextsResponse.Text> tokenize(String query, boolean english, boolean useStricktToken, int inputTokenizeType) throws IOException
    {

        Tokenizer tok = new Tokenizer(inputTokenizeType, useStricktToken, this.tokLex);
		List<TextsResponse.Text> result = new ArrayList<>();
        segmentizer.segmentize(query);
        while(segmentizer.hasMoreSentences())
        {
           String sentenceStr = segmentizer.getNextSentence();
           tok.tokenize(sentenceStr);
           if(tok.tokens.size() <= 0)
               continue;

           tok.splitAbbreviations();

		   List<TextsResponse.Text> tokensInSentence = new ArrayList<>();
           for(Object token : tok.tokens)
             tokensInSentence.add(new TextsResponse.Text().withContent(((TokenTags)token).lexeme));

			result.add(new TextsResponse.Text().withTexts(tokensInSentence).withRole("sentence"));

       }
		return result;
    }

    @Override
	protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
	{
        boolean functions=false, phraseLine=false, sentLine=false, markUnknown=false;
        boolean /*useHybrid=false,*/ showLemma=false, showTokenization=false, strictTokenization=false;
        boolean mergeLabels=false, featureAgreement=false, showErrors=false;
        IceTagger.HmmModelType modelType = IceTagger.HmmModelType.none;


        // Get the request handles
//	response.setContentType("application/json");
//	response.setCharacterEncoding("UTF-8");
//        PrintWriter out = response.getWriter();

		TextRequest elgRequest = null;
		try {
			InputStream inputStream = request.getInputStream();
			elgRequest = mapper.readValue(inputStream, TextRequest.class);
		} catch (Exception e) {
			// invalid request
			response.setContentType("application/json");
			response.setStatus(400);
			mapper.writeValue(response.getOutputStream(), new Failure().withErrors(StandardMessages.elgRequestInvalid()).asMessage());
			return;
		}

        String query = elgRequest.getContent();
        
        //boolean english = (request.getParameter("english").equals("true"));
        boolean english = true;
	/*
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
        if(json_request.has("inputTokenize")){
        	inputTokenizeType = json_request.getInt("inputTokenize");
	}
*/
	int inputTokenizeType = 0;
        // Return the fully tagged and parsed string
	//response.setContentType("text/html;charset="+defaultEncoding);
//	out.write("{");
//	out.write("\"response\":{");
//	out.write("\"type\":\"texts\"");
	try {
		// Tag the query
		TextsResponse textsResponse = analyse(query, english, sentLine, markUnknown, functions, phraseLine, mergeLabels, featureAgreement, showErrors, modelType, showTokenization, strictTokenization, inputTokenizeType, showLemma);

		response.setContentType("application/json");
		mapper.writeValue(response.getOutputStream(), textsResponse.asMessage());
	} catch (Exception e) {
		e.printStackTrace();
		response.setContentType("application/json");
		response.setStatus(500);
		mapper.writeValue(response.getOutputStream(), new Failure().withErrors(
				StandardMessages.elgServiceInternalError(e.getMessage())).asMessage());
	}
//	out.write("}");
//	out.write("}");
//	out.flush();
	}

    private void testDict()
    {
       Lexicon baseDict = morphyLex.baseDict;

       for (Enumeration keys = baseDict.keys() ; keys.hasMoreElements() ;) {
          System.out.println(keys.nextElement());
       }
    }
    
    private TextsResponse analyse(String query, boolean english, boolean sentLine, boolean markUnknown,
                                  boolean functions, boolean phraseLine, boolean mergeLabels, boolean featureAgreement, boolean showErrors,
                                  IceTagger.HmmModelType modelType, boolean showTokenization, boolean useStricktToken, int inputTokenizeType, boolean showLemma) throws IOException
    {

        // Only show output of tokenization?
        if (showTokenization)
			return new TextsResponse().withTexts(tokenize(query, english, useStricktToken, inputTokenizeType));
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

            return new TextsResponse().withTexts(writeTaggedText(sents, sentLine, markUnknown, english, showLemma));

	    //out.write(",\"parsed\":\"" + parsed.replaceAll( "\n", "|")+"\"");

		}
    }

    private boolean printWebError( PrintWriter out, String errorstring )
	{
		out.write( errorstring + "\n" );
		return true;
	}
}
