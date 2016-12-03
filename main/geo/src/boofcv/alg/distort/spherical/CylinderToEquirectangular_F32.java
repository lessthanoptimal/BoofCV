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

package boofcv.alg.distort.spherical;

import georegression.misc.GrlConstants;

/**
 * Renders a cylindrical view from an equirectangular image.  With zero rotation applied to it the image center
 * has the pointing vector = (1,0,0).
 *
 * <pre>
 * r = atan(vfov/2) // implicit cylinder radius of 1
 * theta = 2*PI*pixelX/width - PI
 *
 * x = cos(theta)
 * y = sin(theta)
 * z = 2*r*pixelY/(height - 1) - r
 * </pre>
 *
 * @author Peter Abeles
 */
public class CylinderToEquirectangular_F32 extends EquirectangularDistortBase_F32 {

	/**
	 * Configures the rendered cylinder
	 *
	 * @param width Cylinder width in pixels
	 * @param height Cylinder height in pixels
	 * @param vfov vertical FOV in radians
	 */
	public void configure( int width , int height , float vfov ) {
		declareVectors( width, height );

		float r = (float)Math.tan(vfov/2.0f);

		for (int pixelY = 0; pixelY < height; pixelY++) {
			float z = 2*r*pixelY/(height-1) - r;
			for (int pixelX = 0; pixelX < width; pixelX++) {
				float theta = GrlConstants.F_PI2*pixelX/width - GrlConstants.F_PI;
				float x = (float)Math.cos(theta);
				float y = (float)Math.sin(theta);

				vectors[pixelY*width+pixelX].set(x,y,z);
			}
		}
	}
}
