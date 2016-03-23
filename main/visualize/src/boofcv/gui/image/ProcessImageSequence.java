/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.image;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageGray;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;


/**
 * An abstract class that takes case of basic GUI and loading of images when processing a sequence.
 *
 * @author Peter Abeles
 */
public abstract class ProcessImageSequence<T extends ImageGray> implements MouseListener, KeyListener {
	private SimpleImageSequence<T> sequence;

	private T image;

	private volatile boolean paused;
	private volatile boolean step;

	protected int imgWidth;
	protected int imgHeight;

	// how many images have been saved
	protected int savedIndex;

	public ProcessImageSequence(SimpleImageSequence<T> sequence) {
		this.sequence = sequence;
		if (sequence.hasNext())
			image = sequence.next();
		else
			throw new IllegalArgumentException("Image sequence must have at least one image in it.");

		this.imgWidth = image.getWidth();
		this.imgHeight = image.getHeight();

		System.out.println("Input Image size = " + imgWidth + " " + imgHeight);
		System.out.println();
	}

	/**
	 * If a component is added here then keyboard and mouse events will be used to control the
	 * image processing.
	 *
	 * @param comp
	 */
	public void addComponent(JComponent comp) {
		comp.addMouseListener(this);
		comp.addKeyListener(this);
	}

	public void process() {
		long totalTime = 0;
		int numFrames = 0;

		paused = false;
		step = false;

		long startNano = System.nanoTime();
		while (true) {

			long before = System.nanoTime();
			processFrame(image);
			long after = System.nanoTime();

			// don't compute time statistics on the first frame since it seems to
			// often be much slower

			if (numFrames > 0) {
				totalTime += after - before;
				printFPS(totalTime, numFrames, startNano);
			} else {
				System.out.println("First frame processed.");
			}

			numFrames++;

			updateGUI((BufferedImage)sequence.getGuiImage(), image);

			while (paused) {
				if (step) {
					step = false;
					break;
				}
				Thread.yield();
			}

//            if( stabilizer.getKeyFrameChanged() && numFrames > 2 ) {
////            if( numFrames > 80 ) {
//                try {
//                    Thread.sleep(200000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
			if (sequence.hasNext()) {
				image = sequence.next();
			} else {
				break;
			}
		}

		finishedSequence();
		printFPS(totalTime, numFrames, startNano);
		sequence.close();
	}

	private void printFPS(long totalTime, int numFrames, long startNano) {
		double seconds = totalTime / 1e9;
		double allSeconds = (System.nanoTime() - startNano) / 1e9;
		double fps = numFrames / seconds;
		double tfps = numFrames / allSeconds;
		System.out.printf("Frame # = %5d FPS = %6.2f TFPS = %6.2f ET = %7.1f\n", numFrames, fps, tfps, allSeconds);
	}

	public abstract void processFrame(T image);

	public abstract void updateGUI(BufferedImage guiImage, T origImage);

	/**
	 * Called after all the frames in the sequence have been processed.
	 */
	public void finishedSequence(){}


	@Override
	public void mouseClicked(MouseEvent e) {
		// thsi is needed so that key events are processed
		if (e.getSource() instanceof JComponent) {
			JComponent jc = (JComponent) e.getSource();

			jc.requestFocusInWindow();
		}

		paused = !paused;
		System.out.println("Pause = " + paused);
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() == 'p') {
			paused = false;
		} else if (e.getKeyChar() == 's') {
			System.out.println("Saving image");
			String name = String.format("image%05d.jpg", savedIndex++);

			UtilImageIO.saveImage(image, name);
		} else {
			paused = true;
			step = true;
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
}
