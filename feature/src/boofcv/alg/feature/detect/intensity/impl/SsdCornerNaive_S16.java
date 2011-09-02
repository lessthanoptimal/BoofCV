/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.feature.detect.intensity.KltCornerIntensity;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;

/**
 * Naive implementation of {@link boofcv.alg.feature.detect.intensity.KltCornerIntensity} which performs computations in a straight
 * forward but inefficient manor.  This class is used to validate the correctness of more complex but efficient
 * implementations.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class SsdCornerNaive_S16 implements KltCornerIntensity<ImageSInt16> {

	// feature's radius
	private int radius;

	// the intensity of the found features in the image
	private ImageFloat32 featureIntensity;

	public SsdCornerNaive_S16(int imageWidth, int imageHeight,
							  int windowRadius) {
		this.radius = windowRadius;

		featureIntensity = new ImageFloat32(imageWidth, imageHeight);
	}

	@Override
	public int getRadius() {
		return radius;
	}

	@Override
	public ImageFloat32 getIntensity() {
		return featureIntensity;
	}


	@Override
	public void process(ImageSInt16 derivX, ImageSInt16 derivY) {

		final int imgHeight = derivX.getHeight();
		final int imgWidth = derivX.getWidth();

		for (int row = radius; row < imgHeight - radius; row++) {
			for (int col = radius; col < imgWidth - radius; col++) {
				int dxdx = 0;
				int dxdy = 0;
				int dydy = 0;

				for (int i = -radius; i <= radius; i++) {
					for (int j = -radius; j <= radius; j++) {
						int dx = derivX.get(col + j, row + i);
						int dy = derivY.get(col + j, row + i);

						dxdx += dx * dx;
						dydy += dy * dy;
						dxdy += dx * dy;
					}
				}

				// compute the eigen values
				double left = (dxdx + dydy) * 0.5;
				double b = (dxdx - dydy) * 0.5;
				double right = Math.sqrt(b * b + (double)dxdy * dxdy);

				featureIntensity.set(col, row, (float) (left - right));
			}
		}
	}

}
