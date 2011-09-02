/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestDistortImageOps {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	@Test
	public void scale() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 output = new ImageFloat32(width,height);
		ImageFloat32 output2 = new ImageFloat32(width,height);

		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(input);

		// check the two scale function
		DistortImageOps.scale(input,output,interp);
		DistortImageOps.scale(input,output2, TypeInterpolate.BILINEAR);

		// they should be identical
		BoofTesting.assertEquals(output,output2);

		interp.setImage(input);

		float scaleX = (float)input.width/(float)output.width;
		float scaleY = (float)input.height/(float)output.height;

		if( input.getTypeInfo().isInteger() ) {
			for( int i = 0; i < output.height; i++ ) {
				for( int j = 0; j < output.width; j++ ) {
					float val = interp.get(j*scaleX,i*scaleY);
					assertEquals((int)val,output.get(j,i),1e-4);
				}
			}
		} else {
			for( int i = 0; i < output.height; i++ ) {
				for( int j = 0; j < output.width; j++ ) {
					float val = interp.get(j*scaleX,i*scaleY);
					assertEquals(val,output.get(j,i),1e-4);
				}
			}
		}
	}

	@Test
	public void rotate() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 output = new ImageFloat32(width,height);
		ImageFloat32 output2 = new ImageFloat32(width,height);

		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(input);

		int middleX = input.width/2;
		int middleY = input.height/2;

		DistortImageOps.rotate(input,output,interp,middleX,middleY,(float)Math.PI/2f);
		DistortImageOps.rotate(input,output2,TypeInterpolate.BILINEAR,middleX,middleY,(float)Math.PI/2f);

		// they should be identical
		BoofTesting.assertEquals(output,output2);

		// check for a 90 degrees rotation
		assertEquals(input.get(middleX+5,middleY+3),output.get(middleX+3,middleY+5),1e-4);
		assertEquals(input.get(middleX+4,middleY+6),output.get(middleX+6,middleY+4),1e-4);
	}
}
