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

package boofcv.demonstrations.sfm.d2;

import georegression.struct.homography.Homography2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays stabilized images
 *
 * @author Peter Abeles
 */
public class Stabilize2DPanel extends Motion2DPanel {

	private static int outputBorder = 20;

	private int imageWidth;

	// Buffered image with the combined output
	BufferedImage combined = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	public void setInputSize( int width, int height ) {
		imageWidth = width;
		windowWidth = 2*width + outputBorder;
		windowHeight = height;

		combined = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_ARGB);
		setPreferredSize(new Dimension(windowWidth, windowHeight));
		setMinimumSize(getPreferredSize());
	}

	@Override
	public void updateImages( BufferedImage input, BufferedImage stitched ) {
		Graphics2D g2 = combined.createGraphics();
		g2.drawImage(input, 0, 0, null);
		g2.drawImage(stitched, outputBorder + imageWidth, 0, null);
		setImage(combined);
	}

	@Override
	protected void drawFeatures( Graphics2D g2 ) {
		drawFeatures(0, allTracks, inliers, currToWorld, g2);
		drawFeatures(outputBorder + imageWidth, allTracks, inliers, new Homography2D_F64(), g2);
	}
}
