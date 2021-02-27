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
	protected void drawFeatures( Graphics2D g2  ) {
		drawFeatures(allTracks,inliers,currToWorld,g2);
	}
}
