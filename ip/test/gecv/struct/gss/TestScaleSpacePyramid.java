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

package gecv.struct.gss;

import gecv.alg.interpolate.InterpolatePixel;
import gecv.factory.interpolate.FactoryInterpolation;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageUInt8;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.PyramidUpdater;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestScaleSpacePyramid {

	/**
	 * Make sure the scale has been internally reduced by 1/2
	 */
	@Test
	public void checkScalingHalved() {
		// see if all the layers are set correctly
		InterpolatePixel<ImageUInt8> interp = FactoryInterpolation.bilinearPixel(ImageUInt8.class);
		ScaleSpacePyramid<ImageUInt8> pyramid = new ScaleSpacePyramid<ImageUInt8>(interp,1,2,5.5);

		for( int i = 0; i < pyramid.getNumLayers(); i++ )
			assertEquals(pyramid.scale[i]*2,pyramid.getScale(i),1e-5);
	}

	public static class DoNothingUpdater<T extends ImageBase> implements PyramidUpdater<T>
	{
		@Override
		public void update(T input, ImagePyramid<T> imagePyramid) {
		}
	}
}
