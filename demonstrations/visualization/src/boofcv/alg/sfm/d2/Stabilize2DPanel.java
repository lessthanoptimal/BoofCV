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

package boofcv.alg.sfm.d2;

import georegression.struct.homography.Homography2D_F64;

import java.awt.*;

/**
 * @author Peter Abeles
 */
public class Stabilize2DPanel extends Motion2DPanel {

	private static int outputBorder = 20;

	public void setInputSize( int width , int height ) {
		windowWidth = 2*width + outputBorder;
		windowHeight = height;

		setPreferredSize(new Dimension(windowWidth,windowHeight));
		setMinimumSize(getPreferredSize());
	}

	@Override
	protected void drawImages( double scale , Graphics2D g2 ) {
		int scaledInputWidth = (int)(scale*input.getWidth());
		int scaledInputHeight = (int)(scale*input.getHeight());

		int scaledOutputWidth = (int)(scale* stitched.getWidth());
		int scaledOutputHeight = (int)(scale* stitched.getHeight());

		distortOffX = scaledInputWidth + outputBorder;

		// draw undistorted on left
		g2.drawImage(input,0,0,scaledInputWidth,scaledInputHeight,0,0,input.getWidth(),input.getHeight(),null);

		// draw distorted on right
		g2.drawImage(stitched,scaledInputWidth+outputBorder,0,
				scaledInputWidth+scaledOutputWidth+outputBorder,scaledOutputHeight,
				0,0,stitched.getWidth(),stitched.getHeight(),null);
	}

	@Override
	protected void drawFeatures( float scale, Graphics2D g2  ) {
		int scaledInputWidth = (int)(scale*input.getWidth());

		drawFeatures(scale,0,0,allTracks,inliers,new Homography2D_F64(),g2);
		drawFeatures(scale,scaledInputWidth+outputBorder,0,allTracks,inliers,currToWorld,g2);
	}
}
