/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.gui;

import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.calib.VisualDepthParameters;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 * Base class for processing sequences of Gray and Depth images.
 *
 * @author Peter Abeles
 */
public abstract class DepthVideoAppBase<I extends ImageSingleBand, Depth extends ImageSingleBand>
		extends SelectAlgorithmAndInputPanel implements VisualizeApp, MouseListener, ChangeListener
{
	protected VisualDepthParameters config;
	protected SimpleImageSequence<I> sequence1;
	protected SimpleImageSequence<Depth> sequence2;

	volatile boolean requestStop = false;
	volatile boolean isRunning = false;
	volatile boolean isPaused = false;

	long framePeriod = 100;

	JSpinner periodSpinner;

	protected Class<I> imageType;
	protected Class<Depth> depthType;

	public DepthVideoAppBase(int numAlgFamilies, Class<I> imageType, Class<Depth> depthType) {
		super(numAlgFamilies);

		this.imageType = imageType;
		this.depthType = depthType;
		addToToolbar(createSelectDelay());
	}

	private JPanel createSelectDelay() {
		JPanel ret = new JPanel();
		ret.setLayout(new BoxLayout(ret, BoxLayout.X_AXIS));

		periodSpinner = new JSpinner(new SpinnerNumberModel(framePeriod,0,1000,10));
		periodSpinner.setMaximumSize(periodSpinner.getPreferredSize());
		periodSpinner.addChangeListener(this);

		ret.add(new JLabel("Delay"));
		ret.add(periodSpinner);

		return ret;
	}

	public void startWorkerThread() {
		new WorkThread().start();
	}

	protected abstract void process( SimpleImageSequence<I> sequence1,
									 SimpleImageSequence<Depth> sequence2 );

	protected abstract void updateAlg(I frame1, BufferedImage buffImage1,
									  Depth frame2, BufferedImage buffImage2 );

	protected abstract void updateAlgGUI( I frame1 , BufferedImage buffImage1 ,
										  Depth frame2 , BufferedImage buffImage2 ,double fps );

	@Override
	public void changeInput(String name, int index) {
		stopWorker();
		Reader r = media.openFile(inputRefs.get(index).getPath());
		BufferedReader in = new BufferedReader(r);
		try {
			String path = new File(inputRefs.get(index).getPath()).getParent();

			String lineConfig = in.readLine();
			String line1 = in.readLine();
			String line2 = in.readLine();

			// adjust for relative paths
			if( lineConfig.charAt(0) != '/' )
				lineConfig = path+"/"+lineConfig;
			if( line1.charAt(0) != '/' )
				line1 = path+"/"+line1;
			if( line2.charAt(0) != '/' )
				line2 = path+"/"+line2;

			config = UtilIO.loadXML(media.openFile(lineConfig));
			SimpleImageSequence<I> video1 = media.openVideo(line1, ImageType.single(imageType));
			SimpleImageSequence<Depth> video2 = media.openVideo(line2, ImageType.single(depthType));

			process(video1,video2);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void stopWorker() {
		requestStop = true;
		while( isRunning ) {
			Thread.yield();
		}
		requestStop = false;
	}

	private class WorkThread extends Thread
	{
		@Override
		public void run() {
			isRunning = true;

			long totalTrackerTime = 0;
			long totalFrames = 0;

			handleRunningStatus(0);

			while( requestStop == false ) {
				long startTime = System.currentTimeMillis();
				if( !isPaused ) {
					// periodically reset the FPS
					if( totalFrames > 20 ) {
						totalFrames = 0;
						totalTrackerTime = 0;
					}

					if( sequence1.hasNext() && sequence2.hasNext() ) {
						I frame1 = sequence1.next();
						Depth frame2 = sequence2.next();

						BufferedImage buffImage1 = sequence1.getGuiImage();
						BufferedImage buffImage2 = sequence2.getGuiImage();

						long startTracker = System.nanoTime();
						updateAlg(frame1, buffImage1,frame2, buffImage2);
						totalTrackerTime += System.nanoTime()-startTracker;
						totalFrames++;

						updateAlgGUI(frame1,buffImage1,frame2,buffImage2,
								1e9/(totalTrackerTime/totalFrames));

						gui.repaint();

					} else {
						break;
					}
				}
				while( System.currentTimeMillis()-startTime < framePeriod ) {
					synchronized (this) {
						try {
							long period = System.currentTimeMillis()-startTime-10;
							if( period > 0 )
								wait(period);
						} catch (InterruptedException e) {
						}
					}
				}
			}

			isRunning = false;
			handleRunningStatus(2);
		}
	}

	/**
	 * 0 = running
	 * 1 = paused
	 * 2 = finished
	 */
	abstract protected void handleRunningStatus( int status );

	@Override
	public void mouseClicked(MouseEvent e) {
		if( !isRunning )
			return;

		isPaused = !isPaused;

		if( isPaused )
			handleRunningStatus(1);
		else
			handleRunningStatus(0);
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == periodSpinner ) {
			framePeriod = ((Number)periodSpinner.getValue()).intValue();
		}
	}

}
