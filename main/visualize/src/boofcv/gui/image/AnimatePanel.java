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

package boofcv.gui.image;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays a sequence of images.
 *
 * @author Peter Abeles
 */
public class AnimatePanel extends JPanel {

	BufferedImage images[];
	long previousTime;
	int period;
	int frame;

	Timer timer;

	public AnimatePanel( int period , BufferedImage... images) {
		this.period = period;
		this.images = images;
		if( images != null )
			setPreferredSize(new Dimension(images[0].getWidth(),images[1].getHeight()));
	}

	public void setAnimation( BufferedImage... images ) {
		this.images = images;
	}

	public void start() {
		if( timer != null )
			throw new IllegalArgumentException("Already running");

		timer = new Timer();
		timer.start();
	}

	public void stop() {
		timer.running = false;
		timer = null;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		if( images == null )
			return;

		if( previousTime <= System.currentTimeMillis() ) {
			previousTime = System.currentTimeMillis()+period-1;
			frame = (frame+1)%images.length;
		}

		g2.drawImage(images[frame], 0, 0, this);
	}

	private class Timer extends Thread {

		public volatile boolean running = true;

		@Override
		public void run() {
			previousTime = 0;
			synchronized(this){
				while( running ) {
					try {
						wait(period);
						repaint();
					} catch (InterruptedException e) {}
				}
			}
		}
	}
}
