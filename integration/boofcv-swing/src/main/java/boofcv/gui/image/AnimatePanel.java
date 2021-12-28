/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.misc.BoofMiscOps;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays a sequence of images.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class AnimatePanel extends JPanel {
	BufferedImage[] images;
	long previousTime;
	int period;
	int frame;

	@Nullable Timer timer;

	public AnimatePanel( int period, BufferedImage... images ) {
		this.period = period;
		if (images.length > 0) {
			this.images = images;
			setPreferredSize(new Dimension(images[0].getWidth(), images[0].getHeight()));
		}
	}

	public void setAnimation( BufferedImage... images ) {
		if (images.length == 0)
			throw new IllegalArgumentException("Can't be of length 0");
		this.frame = 0;
		this.images = images;
	}

	public AnimatePanel start() {
		if (timer != null)
			throw new IllegalArgumentException("Already running");

		timer = new Timer();
		timer.start();
		return this;
	}

	public void stop() {
		if (timer == null)
			return;
		timer.running = false;
		timer = null;
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		if (images == null)
			return;

		if (previousTime <= System.currentTimeMillis()) {
			previousTime = System.currentTimeMillis() + period - 1;
			frame = (frame + 1)%images.length;
		}

		g2.drawImage(images[frame], 0, 0, this);
	}

	private class Timer extends Thread {

		public volatile boolean running = true;

		@Override
		public void run() {
			previousTime = 0;
			synchronized (this) {
				while (running) {
					BoofMiscOps.sleep(period);
					repaint();
				}
			}
		}
	}
}
