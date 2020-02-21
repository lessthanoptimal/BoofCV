/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.GImageStatistics;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.ImageGray;

import javax.swing.*;
import java.awt.*;

/**
 * Draws a histogram of the image's pixel intensity level
 */
public class ImageHistogramPanel extends JPanel {
	private final Object lock = new Object();
	protected int totalBins;
	protected double maxValue;
	protected int bins[];

	public int marker = -1;
	public boolean approximateHistogram=true;

	public ImageHistogramPanel(int totalBins, double maxValue) {
		this.totalBins = totalBins;
		this.maxValue = maxValue;
		this.bins = new int[ totalBins ];
	}

	/**
	 * Update that can be called at any time. Will lock UI thread until it's done
	 * @param image
	 */
	public void updateSafe( ImageGray image ) {
		synchronized (lock) {
			update(image);
		}
	}

	/**
	 * Update's the histogram. Must only be called in UI thread
	 */
	public void update( ImageGray image ) {
		if( approximateHistogram ) {
			for (int i = 0; i < bins.length; i++)
				bins[i] = 0;

			if (image instanceof GrayF32)
				update((GrayF32) image);
			else if (GrayI.class.isAssignableFrom(image.getClass()))
				update((GrayI) image);
			else
				throw new IllegalArgumentException("Image type not yet supported");
		} else {
			GImageStatistics.histogram(image,0,bins);
		}
	}

	private void update( GrayF32 image ) {
		int maxIndex = totalBins-1;
		if( image.width*image.height < 200*200 ) {
			for (int y = 0; y < image.height; y++) {
				for (int x = 0; x < image.width; x++) {
					int index = (int) (maxIndex * (image.unsafe_get(x, y) / maxValue));
					bins[index]++;
				}
			}
		} else {
			int periodX = (int)Math.ceil(image.width/250.0);
			int periodY = (int)Math.ceil(image.width/250.0);

			for (int y = 0; y < image.height; y += periodY) {
				for (int x = 0; x < image.width; x += periodX ) {
					int index = (int) (maxIndex * (image.unsafe_get(x, y) / maxValue));
					bins[index]++;
				}
			}
		}
	}

	private void update( GrayI image ) {
		int maxIndex = totalBins-1;
		int maxValue = (int)this.maxValue;
		if( image.width*image.height < 200*200 ) {
			for (int y = 0; y < image.height; y++) {
				for (int x = 0; x < image.width; x++) {
					int index = maxIndex * (image.unsafe_get(x, y) / maxValue);
					bins[index]++;
				}
			}
		} else {
			int periodX = (int)Math.ceil(image.width/250.0);
			int periodY = (int)Math.ceil(image.width/250.0);

			for (int y = 0; y < image.height; y += periodY) {
				for (int x = 0; x < image.width; x += periodX ) {
					int index = maxIndex * image.unsafe_get(x, y) / maxValue;
					bins[index]++;
				}
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.white);
		g2.fillRect(0,0,getWidth(),getHeight());

		int w = getWidth();
		int h = getHeight();

		synchronized (lock) {
			int maxCount = 0;
			int histSum = 0;
			for (int i = 0; i < totalBins; i++) {
				histSum += bins[i];
				if (bins[i] > maxCount)
					maxCount = bins[i];
			}

			if (maxCount == 0)
				return;

			// one bin absolutely dominates. Set it to something more reasonable so the histogram can be seen
			if( maxCount >= histSum/10 ) {
				maxCount = (int)Math.sqrt(histSum)/5+2;
			}

			g2.setColor(Color.BLACK);

			for (int i = 0; i < totalBins; i++) {
				int x1 = w * i / totalBins;
				int x2 = w * (i + 1) / totalBins;
				int y = h - h * bins[i] / maxCount;
				g2.fillRect(x1, y, (x2 - x1), h - y);
			}
		}

		int marker = this.marker;
		if( marker >= 0 ) {
			int x = marker*w/totalBins;
			g2.setColor(Color.RED);
			g2.fillRect(x-1,0,3,h);
		}
	}

	public void setMarker(int marker) {
		this.marker = marker;
	}

	public boolean isApproximateHistogram() {
		return approximateHistogram;
	}

	public void setApproximateHistogram(boolean approximateHistogram) {
		this.approximateHistogram = approximateHistogram;
	}
}
