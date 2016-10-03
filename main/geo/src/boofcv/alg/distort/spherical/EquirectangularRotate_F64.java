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

/**
 * Transforms the equirectangular image as if the input image was taken by the camera at the same location but with
 * a rotation.    Includes a built in function to center the camera at a particular location to minimize the distortion.
 *
 * @author Peter Abeles
 */
public class EquirectangularRotate_F64 extends EquirectangularDistortBase_F64 {



	/**
	 * Specifies the image's width and height
	 *
	 * @param width Image width
	 * @param height Image height
	 */
	public void setImageShape( int width , int height ) {
		tools.configure(width, height);
		declareVectors(width, height);

		// precompute vectors for each pixel
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				tools.equiToNormFV(x,y,vectors[y*width+x]);
			}
		}

	}
}
