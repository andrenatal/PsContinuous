package com.pocketsphinxapi.consumer;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;

import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.pocketsphinx;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/*
 *  This class shows the capture of speech and the consuming 
 *  of pocketsphinx api to do speech recognition in various languages  
 *  using the code and tools provided by CMU Sphinx http://cmusphinx.sourceforge.net/
 * 
 *  Andre Natal < andre@allapps.com.br>
 *  20/05/2011
 */
public class MainActivity extends Activity implements OnTouchListener, RecognitionListener, Runnable  {
	
	boolean done = false; 

	Decoder ps;
	private boolean isRecording;
	static {
		try
		{
			System.loadLibrary("pocketsphinx_jni");
			int a = 1;
		}
		catch (Exception exc)
		{
			
			
		}
	}	
	
	GrammarTools gram;
	
	/**
	 * Recognizer task, which runs in a worker thread.
	 */
	RecognizerTask rec;
	/**
	 * Thread in which the recognizer task runs.
	 */
	Thread rec_thread;
	/**
	 * Time at which current recognition started.
	 */
	Date start_date;
	/**
	 * Number of seconds of speech.
	 */
	float speech_dur;
	/**
	 * Are we listening?
	 */
	boolean listening;
	/**
	 * Progress dialog for final recognition.
	 */
	ProgressDialog rec_dialog;
	/**
	 * Performance counter view.
	 */
	TextView performance_text;
	/**
	 * Editable text view.
	 */
	EditText edit_text;
	
	Button bgram; 
	Button brec;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
		try 
		{
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.main);

			// plug events
	        brec = (Button)findViewById(R.id.buttonrec);
	        brec.setOnTouchListener(this);
			
			bgram = (Button)findViewById(R.id.buttongr);			
			bgram.setOnTouchListener(this);
			
			//this.performance_text = (TextView) findViewById(R.id.PerformanceText);
			this.edit_text = (EditText)findViewById(R.id.tbx2);
	        	       
			try {
				
				gram = new GrammarTools("en_US"); 
				gram.InstallModels("en_US",getResources());
				
				//gram = new GrammarTools("es"); 
				//gram.InstallModels("es" , getResources());

				//gram = new GrammarTools("pt_BR"); 
				//gram.InstallModels("pt_BR",getResources());
								
				this.rec = new RecognizerTask(gram);
				this.rec.setRecognitionListener(this);
				this.rec_thread = new Thread(this.rec);
				this.listening = false;					
				this.rec_thread.start(); 	        
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        	        				
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		       
    }
       	
	@Override
	public boolean onTouch(View arg0, MotionEvent event) 
	{
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			
			if (arg0.equals(brec))
			{
				try {				
					this.rec.doRecognize(); 				
					start_date = new Date();
					this.listening = true;
					this.rec.start();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				

			}
			
			if (arg0.equals(bgram))
			{							
				try {
					
					
					this.rec.Reset();
					this.rec_dialog = ProgressDialog.show(this, "Working", "Generating grammar ..", true,false);
					Thread thread = new Thread(this);
	                thread.start();
					

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				

			}
			
			break;
		case MotionEvent.ACTION_UP:
			if (arg0.equals(brec))
			{
				
				try
				{
					
					Date end_date = new Date();
					long nmsec = end_date.getTime() - start_date.getTime();
					this.speech_dur = (float)nmsec / 1000;
					if (this.listening) {
						Log.d(getClass().getName(), "Showing Dialog");
						this.rec_dialog = ProgressDialog.show(MainActivity.this, "", "Recognizing speech...", true);
						this.rec_dialog.setCancelable(false);
						this.listening = false;
					}
					this.rec.stop(); 
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 				
				
			}
			break;
			
		default:
			;
		} 
		/* Let the button handle its own state */ 
		return false;		
	}

	@Override
	public void onError(int err) {
		final MainActivity that = this;
		that.edit_text.post(new Runnable() {
			public void run() {
				that.rec_dialog.dismiss();
			}
		});
		
	}

	@Override
	public void onPartialResults(Bundle b) {
		final MainActivity that = this;
		String _hyp = "";
		try
		{
			_hyp = b.getString("hyp");
		} 
		catch (Exception exc)
		{
			System.out.println(exc.getLocalizedMessage());	
		}
		final String hyp = _hyp;
		that.edit_text.post(new Runnable() {
			public void run() {
				try
				{
					that.edit_text.setText(hyp);					
				} 
				catch (Exception exc)
				{
					System.out.println(exc.getLocalizedMessage());	
				}
			}
		});
		
	}

	@Override
	public void onResults(Bundle b) {
		final MainActivity that = this;
		String _hyp = "";
		try
		{
			_hyp = b.getString("hyp");
			System.out.println(_hyp);
		} 
		catch (Exception exc)
		{
			System.out.println(exc.getLocalizedMessage());	
		}
		final String hyp = _hyp;
		this.edit_text.post(new Runnable() {
			public void run() {
				try
				{
					that.edit_text.setText(hyp);					
					Date end_date = new Date();
					long nmsec = end_date.getTime() - that.start_date.getTime();
					float rec_dur = (float)nmsec / 1000;
					//that.performance_text.setText(String.format("%.2f seconds %.2f xRT", that.speech_dur, rec_dur / that.speech_dur));
					Log.d(getClass().getName(), "Hiding Dialog");
					that.rec_dialog.dismiss();
				} 
				catch (Exception exc)
				{
					System.out.println(exc.getLocalizedMessage());	
				}
				
			}
		});
		
	}

	@Override
	public void run() {
		
		// HERE YOU ADD YOUR WORDS
		EditText gram_text = (EditText)findViewById(R.id.tbxg);
		
		ArrayList<String> words = new ArrayList<String>();
		String[] grm = gram_text.getText().toString().split("\n");
		
		for (int i = 0 ; i <= grm.length - 1 ; i++)
		{								
			words.add(grm[i]);
		}
		
		gram.gengram(words); 
		handler.sendEmptyMessage(0);
	}

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	
        	rec_dialog.dismiss();
        	rec_dialog = null;
        }
};
	
	

}