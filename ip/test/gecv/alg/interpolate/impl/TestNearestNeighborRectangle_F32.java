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

package gecv.alg.interpolate.impl;

import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.interpolate.InterpolateRectangle;
import gecv.factory.interpolate.FactoryInterpolation;
import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class TestNearestNeighborRectangle_F32 extends GeneralBilinearRectangleChecks<ImageFloat32>{


	public TestNearestNeighborRectangle_F32() {
		super(ImageFloat32.class);
	}

	@Override
	public InterpolatePixel<ImageFloat32> createPixelInterpolate() {
		return FactoryInterpolation.nearestNeighborPixel(imageType);
	}

	@Override
	public InterpolateRectangle<ImageFloat32> createRectangleInterpolate() {
		return FactoryInterpolation.nearestNeighborRectangle(imageType);
	}

	@Override
	protected ImageFloat32 createImage(int width, int height) {
		return new ImageFloat32(width,height);
	}
}
