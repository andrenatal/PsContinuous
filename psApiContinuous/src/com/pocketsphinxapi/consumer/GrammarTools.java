package com.pocketsphinxapi.consumer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.pocketsphinx;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.MultiAutoCompleteTextView.CommaTokenizer;

public class GrammarTools  {
	
	public String pathdic;
	public String pathgram;
	public String lang;
	public String pathhmm;
	Resources resources;
	public boolean recycledecoder;
	private int dictbase;
	private Hashtable<String, Hashtable<String, String>> htDict;
	private Context _context;
	
	public GrammarTools (String language , Context _context)
	{
		lang = language;
		pathgram = String.format(  "/sdcard/Android/data/pocketsphinx/lm/%s/gramj.txt" , language );
		pathdic = String.format(  "/sdcard/Android/data/pocketsphinx/lm/%s/dic.dic" , language );
		pathhmm = String.format( "/sdcard/Android/data/pocketsphinx/hmm/%s/" , language);

		if (language.equals("en_US")) dictbase = R.raw.cmudict07a;
		if (language.equals("es")) dictbase = R.raw.voxforge_es_sphinx_fix;
		
		this._context = _context;
	}

	public void InstallModels(String Language , Resources myResources) throws Exception
    {
		resources = myResources;

    	// primeiro cria os diretorios
    	File sdCard = Environment.getExternalStorageDirectory();
    	File dirAm = new File (sdCard.getAbsolutePath() + String.format ("/Android/data/pocketsphinx/hmm/%s/" , Language ));
    	dirAm.mkdirs();    	
    	File dirLm = new File (sdCard.getAbsolutePath() + String.format ("/Android/data/pocketsphinx/lm/%s/", Language));
    	dirLm.mkdirs();
    	    	    
    	
    	// agora copia o am
    	if (Language.equals("en_US"))
    	{
	    	CopyFileTo(myResources.openRawResource(R.raw.feat),new FileOutputStream(new File(dirAm,"feat.params")));    	
	    	CopyFileTo(myResources.openRawResource(R.raw.means),new FileOutputStream(new File(dirAm,"means")));
	    	CopyFileTo(myResources.openRawResource(R.raw.noisedict),new FileOutputStream(new File(dirAm,"noisedict")));    	
	    	CopyFileTo(myResources.openRawResource(R.raw.sendump),new FileOutputStream(new File(dirAm,"sendump")));    	
	    	CopyFileTo(myResources.openRawResource(R.raw.transitionmatrices),new FileOutputStream(new File(dirAm,"transition_matrices")));
	    	CopyFileTo(myResources.openRawResource(R.raw.variances),new FileOutputStream(new File(dirAm,"variances")));    	
	    	CopyFileTo(myResources.openRawResource(R.raw.mdef),new FileOutputStream(new File(dirAm,"mdef")));  		
	    	
	    	// copia para os bancos
	    	CopyFileTo(myResources.openRawResource(R.raw.enus),new FileOutputStream(new File("/data/data/com.pocketsphinxapi.consumer/databases/","enus.db")));
	    	
    	}
    	else if (Language.equals("es"))   		
    	{    		
    		CopyFileTo(myResources.openRawResource(R.raw.mixtureweightses),new FileOutputStream(new File(dirAm,"mixture_weights")));
	    	CopyFileTo(myResources.openRawResource(R.raw.feates),new FileOutputStream(new File(dirAm,"feat.params")));    	
	    	CopyFileTo(myResources.openRawResource(R.raw.meanses),new FileOutputStream(new File(dirAm,"means")));
	    	CopyFileTo(myResources.openRawResource(R.raw.noisedictes),new FileOutputStream(new File(dirAm,"noisedict")));    	
	    	CopyFileTo(myResources.openRawResource(R.raw.featuretransform),new FileOutputStream(new File(dirAm,"feature_transform")));    	
	    	CopyFileTo(myResources.openRawResource(R.raw.transitionmatriceses),new FileOutputStream(new File(dirAm,"transition_matrices")));
	    	CopyFileTo(myResources.openRawResource(R.raw.varianceses),new FileOutputStream(new File(dirAm,"variances")));    	
	    	CopyFileTo(myResources.openRawResource(R.raw.mdefes),new FileOutputStream(new File(dirAm,"mdef")));  		    		
    	}
    }		
		
	public void CopyFileTo(InputStream in , FileOutputStream f) throws Exception
    {
        byte[] buffer = new byte[512];
        int len1 = 0;
        while ((len1 = in.read(buffer)) > 0) {
            f.write(buffer, 0, len1);
        }
        f.close();    	    
        in.close();    	    	
    }
	
	
	/**
	 * Generate a grammar with the words as argument 
	 * Can be called only per grammar generation, i.e., you only need to call it again when add news words.     
	 * @param  words  an ArrayList containing all the commands the system should listen
	 */	
	public void gengram( ArrayList<String> comandsarray) 
	{
		try
		{
			
			ArrayList<String> wordList = new ArrayList<String>();
			
			// procura palavras no dict		
			Iterator<String> witr = comandsarray.iterator();
			
			// cria jsgf         
			String jheader = "#JSGF V1.0;grammar simpleExample;public <greet> = %s ; public <completeGreet> = <greet> <greet> <greet> <greet> <greet> <greet> <greet> <greet> <greet> <greet>; ";
			String words = "";
	        while( witr.hasNext() ) 
	        {
	        	words +=  witr.next() + "|";
	        }
			// substitui o identificador pelas palavras da gram        
	        jheader = String.format( jheader , words.substring(0 , words.length() -1));
	        
			// grava jsgf no path definitivo
	        FileWriter outFileG = new FileWriter(pathgram);
	        PrintWriter outpw = new PrintWriter(outFileG);     
	        outpw.println(jheader);
	        outFileG.close();		
			

	    	FileWriter outFile = new FileWriter(pathdic);
	    	PrintWriter out = new PrintWriter(outFile);        	
	        	        
	    	DataBaseHelper dh = new DataBaseHelper(_context , "enus.db" , resources);
	    
	 
		 	try {
		 
		 		dh.openDataBase();
		 
		 	}catch(SQLException sqle){
		 
		 		throw sqle;
		 
		 	}	    	
		    	
	    	
	        witr = comandsarray.iterator();
	        while( witr.hasNext() ) 
	        {
	        	String comando = (String)witr.next();     
	        	String[] splitwords = comando.split(" ");
	        	for (int i = 0 ; i <= splitwords.length - 1; i++)
	        	{	        		
		   	    	 List<String> names = dh.selectAll(splitwords[i]);
			         for (String name : names) {
		        		out.println(name);
			         }
	        	}        	
	        }
	        	        	        	        
        	out.close(); 
	    	out = null;
	    	
        	// o que nao encontrou chama no webservice
        	String exsistingFileName = "/mnt/sdcard/Android/data/gram.txt";
        	FileWriter customdict = new FileWriter(exsistingFileName);
        	PrintWriter printcdict = new PrintWriter(customdict);    
        	for(String word : wordList)
        	{
        		printcdict.println(word);
        	}
        	customdict.close();
        	printcdict.close();
        	customdict = null;
        	printcdict = null;
        	
        	
        	postData();
        	
	    	wordList = null;
	    	
	    	outFileG = null;
	    	outpw = null;
	    	comandsarray = null;
	    	recycledecoder = true;

		} 
		catch (Exception exc)
		{
			System.out.println(exc.getLocalizedMessage());	
		}
		
		
	}

	public void AppendDict(String url) {
		
		/*http://www.speech.cs.cmu.edu/cgi-bin/tools/lmtool.2.pl
		 * 	<INPUT NAME="formtype" TYPE="HIDDEN" value="simple">
	  		<INPUT NAME="corpus" TYPE="FILE" SIZE=60 VALUE="empty">
	  		<INPUT TYPE="submit" VALUE="COMPILE KNOWLEDGE BASE">
		 */

			String urlString = url;
		    String exsistingFileName = "/mnt/sdcard/Android/data/gram.txt";

		    try {
		    	DefaultHttpClient httpclient = new DefaultHttpClient();
		    	File f = new File(exsistingFileName);

		    	HttpPost httpost = new HttpPost(urlString);
		    	MultipartEntity entity = new MultipartEntity();
		    	entity.addPart("formtype", new StringBody("simple"));
		    	entity.addPart("corpus", new FileBody(f));
		    	httpost.setEntity(entity);

		    	HttpResponse response;
		    					
		    	response = httpclient.execute(httpost);
		    	
		    	int status = response.getStatusLine().getStatusCode();
		    		 String mstatus = "";
		    		if (status != HttpStatus.SC_OK) {
		    			mstatus = "FAIL";
		    		} else {
		    			mstatus = "OK";
		    			
		    			String line = "";
		    			StringBuilder total = new StringBuilder();		    			    
	    			    // Wrap a BufferedReader around the InputStream
	    			    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

	    	        	
	    	        	FileWriter fwappenddict = new FileWriter(pathdic,true);
	    	        	PrintWriter printcdict = new PrintWriter(fwappenddict);    
	    			    while ((line = rd.readLine()) != null) 
	    			    { 
	    			    	fwappenddict.write(line + "\n");
	    			    }	
	    			    // Read response until the end	    			    
	    			    fwappenddict.close();
	    	        	printcdict.close();
	    	        	fwappenddict = null;
	    	        	printcdict = null;	    			    
		    		}

		    	if (entity != null) {
		    		entity.consumeContent();
		    	}
		    	
		    } catch (Exception ex) {

		    } finally {
		    	//mDebugHandler.post(mFinishUpload);
		    }
	
	} 
	
	
	public void postData() {
		
		/*http://www.speech.cs.cmu.edu/cgi-bin/tools/lmtool.2.pl
		 * 	<INPUT NAME="formtype" TYPE="HIDDEN" value="simple">
	  		<INPUT NAME="corpus" TYPE="FILE" SIZE=60 VALUE="empty">
	  		<INPUT TYPE="submit" VALUE="COMPILE KNOWLEDGE BASE">
		 */

			String urlString = "http://www.speech.cs.cmu.edu/cgi-bin/tools/lmtool/run";
		    String exsistingFileName = "/mnt/sdcard/Android/data/gram.txt";

		    try {
		    	DefaultHttpClient httpclient = new DefaultHttpClient();
		    	File f = new File(exsistingFileName);

		    	HttpPost httpost = new HttpPost(urlString);
		    	MultipartEntity entity = new MultipartEntity();
		    	entity.addPart("formtype", new StringBody("simple"));
		    	entity.addPart("corpus", new FileBody(f));
		    	httpost.setEntity(entity);

		    	HttpResponse response;
		    					
		    	response = httpclient.execute(httpost);
		    	
		    	int status = response.getStatusLine().getStatusCode();
		    		 String mstatus = "";
		    		if (status != HttpStatus.SC_OK) {
		    			mstatus = "FAIL";
		    		} else {
		    			mstatus = "OK";
		    			
		    			String line = "";
		    			StringBuilder total = new StringBuilder();		    			    
	    			    // Wrap a BufferedReader around the InputStream
	    			    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

	    			    // Read response until the end
	    			    while ((line = rd.readLine()) != null) { 
	    			        total.append(line); 
	    			        if (line.indexOf("speech.cs.cmu.edu") > -1)
	    			        {
	    			        	String url = line.substring( line.indexOf("f=\"")+3 , line.indexOf("z\"")+1).replace("TAR", "").replace("tgz", "dic");
	    			        	AppendDict(url);	  
	    			        	break;
	    			        }
	    			    }		    			    
		    		}

		    	if (entity != null) {
		    		entity.consumeContent();
		    	}
		    	
		    } catch (Exception ex) {

		    } finally {
		    	//mDebugHandler.post(mFinishUpload);
		    }
	
	} 

	private StringBuilder inputStreamToString(InputStream is) throws IOException {
	    String line = "";
	    StringBuilder total = new StringBuilder();
	    
	    // Wrap a BufferedReader around the InputStream
	    BufferedReader rd = new BufferedReader(new InputStreamReader(is));

	    // Read response until the end
	    while ((line = rd.readLine()) != null) { 
	        total.append(line); 
	    }
	    
	    // Return full string
	    return total;
	}
	

}			
