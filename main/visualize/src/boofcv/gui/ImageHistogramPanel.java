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

package boofcv.gui;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.ImageGray;

import javax.swing.*;
import java.awt.*;

/**
 * Draws a histogram of the image's pixel intensity level
 */
public class ImageHistogramPanel extends JPanel {
	protected int totalBins;
	protected double maxValue;
	protected int bins[];

	public ImageHistogramPanel(int totalBins, double maxValue) {
		this.totalBins = totalBins;
		this.maxValue = maxValue;
		this.bins = new int[ totalBins ];
	}

	public void update( ImageGray image ) {

		for( int i = 0; i < bins.length; i++ )
			bins[i] = 0;

		if( image instanceof GrayF32)
			update( (GrayF32)image );
		else if( GrayI.class.isAssignableFrom(image.getClass()) )
			update( (GrayI)image );
		else
			throw new IllegalArgumentException("Image type not yet supported");
	}

	private void update( GrayF32 image ) {
		for( int y = 0; y < image.height; y++ ) {
			for( int x = 0; x < image.width; x++ ) {
				int index = (int)(totalBins*(image.get(x,y)/maxValue));
				if( index >= totalBins || index < 0 )
					System.err.println("Bad index in ImageHistogramPanel");
				else
					bins[index]++;
			}
		}
	}

	private void update( GrayI image ) {
		int max = (int)maxValue;
		for( int y = 0; y < image.height; y++ ) {
			for( int x = 0; x < image.width; x++ ) {
				int index = totalBins*image.unsafe_get(x,y)/max;
				if( index >= totalBins || index < 0 )
					System.err.println("Bad index in ImageHistogramPanel");
				else
					bins[index]++;
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.white);
		g2.fillRect(0,0,getWidth(),getHeight());

		int maxCount = 0;
		for( int i = 0; i < totalBins; i++ )  {
			if( bins[i] > maxCount )
				maxCount = bins[i];
		}

		if( maxCount == 0 )
			return;

		g2.setColor(Color.BLACK);

		int w = getWidth();
		int h = getHeight();

		for( int i = 0; i < totalBins; i++ ) {
			int x1 = w*i/totalBins;
			int x2 = w*(i+1)/totalBins;
			int y =  h-h*bins[i]/maxCount;
			g2.fillRect(x1,y,(x2-x1),h-y);
		}
	}
}
