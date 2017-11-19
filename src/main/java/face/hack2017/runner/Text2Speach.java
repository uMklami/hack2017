package face.hack2017.runner;

import java.util.Locale;

import javax.speech.Central;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.Voice; 

public class Text2Speach {
	static String speaktext; 
	public static void dospeak(String speak,String  voicename)    
	{    
	    speaktext=speak;    
	    String voiceName =voicename;    
	try    
    {    
        System.setProperty("FreeTTSSynthEngineCentral", "com.sun.speech.freetts.jsapi.FreeTTSEngineCentral");
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        Central.registerEngineCentral("com.sun.speech.freetts.jsapi.FreeTTSEngineCentral");
        
        SynthesizerModeDesc desc = new SynthesizerModeDesc(null,"general",  Locale.US,null,null);    
        Synthesizer synthesizer =  Central.createSynthesizer(desc);   
      

        synthesizer.allocate();    
        synthesizer.resume();     
        desc = (SynthesizerModeDesc)  synthesizer.getEngineModeDesc();     
        Voice[] voices = desc.getVoices();      
        Voice voice = new Voice();

        for (int i = 0; i < voices.length; i++)    
        {
            if (voices[i].getName().equals(voiceName))    
            {    
                voice = voices[i];
                break;     
            }     
        }    
        synthesizer.getSynthesizerProperties().setVoice(voice);    
        System.out.print("Speaking : "+speaktext);    
        synthesizer.speakPlainText(speaktext, null);    
        synthesizer.waitEngineState(Synthesizer.QUEUE_EMPTY);    
        synthesizer.deallocate();    
    }    
    catch (Exception e)   
    {    
        String message = " missing speech.properties in " + System.getProperty("user.home") + "\n";    
        System.out.println(""+e);    
        System.out.println(message);    
    }    
	}
	public static void main(String[] args) throws Exception {
		dospeak("What can i help you ","kevin16");
	}

}
