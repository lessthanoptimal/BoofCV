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

package boofcv.gui.binary;

import boofcv.gui.ImageHistogramPanel;

import java.awt.*;

/**
 * Displays the image's histogram and shows the innerlier set for a simple threshold
 */
public class HistogramThresholdPanel extends ImageHistogramPanel {
	// the value which is being used for thresholding
	double threshold;
	// is the threshold up or down
	boolean down;

	public HistogramThresholdPanel(int totalBins, double maxValue) {
		super(totalBins, maxValue);
	}

	public void setThreshold( double threshold , boolean down ) {
		this.threshold = threshold;
		this.down = down;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;

		int x = (int)(getWidth()*threshold/maxValue);
		g2.setColor(new Color(0,0,255,50));
		if( !down ) {
			g2.fillRect(x,0,getWidth(),getHeight());
		} else {
			g2.fillRect(0,0,x,getHeight());
		}

	}
}
