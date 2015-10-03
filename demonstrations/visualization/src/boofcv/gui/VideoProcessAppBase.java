/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/**
 * Base class which handles starting and stopping of video streams for apps that process video streams.
 *
 * @author Peter Abeles
 */
public abstract class VideoProcessAppBase<I extends ImageBase>
		extends SelectAlgorithmAndInputPanel implements VisualizeApp, MouseListener , ChangeListener
{
	protected SimpleImageSequence<I> sequence;

	volatile boolean requestStop = false;
	volatile boolean isRunning = false;
	volatile protected boolean isPaused = false;

	long framePeriod = 100;

	protected JSpinner periodSpinner;

	protected ImageType<I> imageType;

	public VideoProcessAppBase(int numAlgFamilies, Class<I> imageClass) {
		super(numAlgFamilies);

		this.imageType = new ImageType<I>(ImageType.Family.SINGLE_BAND, ImageDataType.classToType(imageClass),1);
		addToToolbar(createSelectDelay());
	}

	public VideoProcessAppBase(int numAlgFamilies, ImageType<I> imageType) {
		super(numAlgFamilies);

		this.imageType = imageType;
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

	protected abstract void process( SimpleImageSequence<I> sequence );

	protected abstract void updateAlg(I frame, BufferedImage buffImage );

	protected abstract void updateAlgGUI( I frame , BufferedImage imageGUI , double fps );

	@Override
	public void changeInput(String name, int index) {
		stopWorker();
		SimpleImageSequence<I> video = media.openVideo(inputRefs.get(index).getPath(), imageType);
		process(video);
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

					if( sequence.hasNext() ) {
						I frame = sequence.next();

						BufferedImage buffImage = sequence.getGuiImage();

						long startTracker = System.nanoTime();
						updateAlg(frame, buffImage);
						totalTrackerTime += System.nanoTime()-startTracker;
						totalFrames++;

						updateAlgGUI(frame,buffImage,1e9/(totalTrackerTime/totalFrames));

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

		setPause(!isPaused);
	}

	protected void setPause( boolean paused ) {
		isPaused = paused;

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
