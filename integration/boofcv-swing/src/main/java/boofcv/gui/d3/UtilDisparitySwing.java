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

package boofcv.gui.d3;

import boofcv.alg.cloud.DisparityToColorPointCloud;

import java.awt.image.BufferedImage;

/**
 * Various utility functions and classes related disparity images and point clouds
 *
 * @author Peter Abeles
 */
public class UtilDisparitySwing {
	public static DisparityToColorPointCloud.ColorImage wrap(BufferedImage image ) {
		return new DisparityToColorPointCloud.ColorImage() {
			@Override
			public boolean isInBounds(int x, int y) {
				return( x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight() );
			}

			@Override
			public int getRGB(int x, int y) {
				return image.getRGB(x,y);
			}
		};
	}
}
