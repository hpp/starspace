package io.hpp.starspace;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import io.hpp.audiolab.AudioIn;
import io.hpp.audiolab.Dirac;

/**
 * A class for handling the input audio and analysis from the mic.
 * and reading and playing audio from midi files.
 * @author izzy
 *
 */
public class Audio {
	AudioIn mAudioIn;
	ProcessHandler mProcessHandler;
	Dirac mDirac;
	DiracCB mDiracCB;
	Object syncObj;
	private List<OnAnalyzedFrequencyChangeListener> freqListeners = new ArrayList<OnAnalyzedFrequencyChangeListener>();
	  
	
	public Audio(){
	}
	
	public Audio init(){
		HandlerThread mThread = new HandlerThread("AudioProcessor");
		mThread.start();
		mProcessHandler = new ProcessHandler(this, mThread.getLooper());
		mAudioIn = new AudioIn(proccessAudio);
		syncObj = new Object();
		mDirac = new Dirac();
		mDiracCB = new DiracCB(syncObj, mDirac);
		return this;
	}
	
	public void startRecord(){
		mAudioIn.start();
	}
	
	double frequency = 0.0;
	
	private void processBuffer(byte[] audioBuffer) {
		mDiracCB.putFeedDirac(audioBuffer);
		double old_frequency = frequency;
		frequency = mDiracCB.getFundamentalFrequency();
		if (old_frequency != frequency){
			fireFreqChangeListeners(frequency);
		}
	}
	
	private void fireFreqChangeListeners(double frequency) {
		for (OnAnalyzedFrequencyChangeListener audience:freqListeners){
			audience.frequencyUpdate(frequency); 
		}
		
	}

	public void addFreqChangeListener(OnAnalyzedFrequencyChangeListener freqListener){
		freqListeners.add(freqListener);
	}
	
	public interface OnAnalyzedFrequencyChangeListener{
		void frequencyUpdate(double newFrequency);
	}
	
	AudioIn.audioInByteBufferListener proccessAudio = new AudioIn.audioInByteBufferListener() {
		
		@Override
		public void audioInByteBuffer(byte[] inAudio) {
			if (mProcessHandler!=null){
				Message procMsg = Message.obtain(mProcessHandler, 18, inAudio);//processorHandler.obtainMessage(newSampleWhat); 
				//procHandlerMsg.obj = in_buff;
				mProcessHandler.sendMessage(procMsg); 
			}
		}
	};	
	
	protected static class ProcessHandler extends Handler {
		private final WeakReference<Audio> audioReference;
		
		public ProcessHandler(Audio aRef, Looper loop){
			super(loop);
			audioReference = new WeakReference<Audio>(aRef);
			
		}
		
		public void handleMessage(Message msg) {
			Audio mAudio = audioReference.get();
			if (mAudio!=null){
				mAudio.processBuffer((byte[]) msg.obj);
			}
		}
	}


}
