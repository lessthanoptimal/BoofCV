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

package gecv.alg.detect.corner.impl;

import gecv.alg.InputSanityCheck;
import gecv.alg.detect.corner.KitRosCornerIntensity;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;

/**
 * <p>
 * Implementation of {@link gecv.alg.detect.corner.KitRosCornerIntensity} based off of {@link SsdCorner_S16}.
 * </p>
 *
 * @author Peter Abeles
 */
public class KitRosCorner_S16 implements KitRosCornerIntensity<ImageSInt16> {

	// the intensity of the found features in the image
	private ImageFloat32 featureIntensity;

	public KitRosCorner_S16() {
	}

	@Override
	public void process(ImageSInt16 derivX, ImageSInt16 derivY,
					 ImageSInt16 hessianXX, ImageSInt16 hessianYY , ImageSInt16 hessianXY ) {
		InputSanityCheck.checkSameShape(derivX,derivY,hessianXX,hessianYY);
		
		final int width = derivX.width;
		final int height = derivY.height;

		if( featureIntensity == null ) {
			featureIntensity = new ImageFloat32(width,height);
		}

		for( int y = 0; y < height; y++ ) {
			int indexX = derivX.startIndex + y*derivX.stride;
			int indexY = derivY.startIndex + y*derivY.stride;
			int indexXX = hessianXX.startIndex + y*hessianYY.stride;
			int indexYY = hessianYY.startIndex + y*hessianYY.stride;
			int indexXY = hessianXY.startIndex + y*hessianXY.stride;

			int indexInten = featureIntensity.startIndex + y*featureIntensity.stride;

			for( int x = 0; x < width; x++ ) {
				int dx = derivX.data[indexX++];
				int dy = derivY.data[indexY++];
				int dxx = hessianXX.data[indexXX++];
				int dyy = hessianYY.data[indexYY++];
				int dxy = hessianXY.data[indexXY++];

				int dx2 = dx*dx;
				int dy2 = dy*dy;


				float top = Math.abs(dxx*dy2 - 2*dxy*dx*dy + dyy*dx2);
				float bottom = dx2 + dy2;

				if( bottom == 0.0 )
					featureIntensity.data[indexInten++] = 0;
				else
					featureIntensity.data[indexInten++] = top/bottom;
			}
		}
	}

	@Override
	public int getRadius() {
		return 0;
	}

	@Override
	public ImageFloat32 getIntensity() {
		return featureIntensity;
	}
}
