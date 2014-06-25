package io.hpp.starspace;

import io.hpp.audiolab.Dirac;

public class DiracCB {
	Object diracSync;
	Dirac mDirac;
	float[] processBuffer;
	protected int buffIdx = 0;
	protected double fundFrequency;

	public DiracCB(Object sync, Dirac dirac){
		diracSync = sync;
		mDirac = dirac;
	}
	
	public interface AudioBufferFillnCB{
		int audioBufferFillnCB(float[] buffer2fill, long numFrames);
	}
	
	public AudioBufferFillnCB charlieBuffill = new AudioBufferFillnCB() {
		
		@Override
		public int audioBufferFillnCB(float[] buffer2fill, long numFrames) {
			synchronized(diracSync){
				int res = (int) numFrames;
				
				while (buffIdx < res){
					try {
						diracSync.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					buffer2fill = processBuffer;
					fundFrequency = mDirac.getFrequencyAnalysis();
					buffIdx = res;
				}
				diracSync.notifyAll();
				return res;
			}
		}
	};
	
	public void putFeedDirac(byte[] audioIn){
		synchronized(diracSync){
			while (buffIdx > 0){
				try {
					diracSync.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				processBuffer = convertByteArrayToFloat(audioIn);
				buffIdx = 0;
			}
			diracSync.notifyAll();
		}
	}

	private float[] convertByteArrayToFloat(byte[] audioIn) {
		float[] floatArray = new float[audioIn.length / 2];
		
		for (int i = 0; i < floatArray.length; i++){
			short tempShort = (short) ((audioIn[2*i+1]<<8) + audioIn[2*i]); 
			floatArray[i] = (float) (tempShort / Math.pow(2,15)); 
		} 
		return floatArray;
	}

	public double getFundamentalFrequency() {
		synchronized(diracSync){
			return fundFrequency;
		}
	}
};
