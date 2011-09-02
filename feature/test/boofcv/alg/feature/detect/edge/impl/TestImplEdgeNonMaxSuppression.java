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

package boofcv.alg.feature.detect.edge.impl;

import boofcv.alg.misc.ImageTestingOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestImplEdgeNonMaxSuppression {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	@Test
	public void naive() {
		ImageFloat32 intensity = new ImageFloat32(3,3);
		ImageFloat32 output = new ImageFloat32(3,3);
		ImageUInt8 direction = new ImageUInt8(3,3);

		// test it against simple positive and  negative cases
		for( int dir = 0; dir < 4; dir++ ) {
			direction.set(1,1,dir);

			GeneralizedImageOps.fill(intensity,0);
			intensity.set(1,1,10);

			// test suppress
			setByDirection(intensity,dir,15);
			ImplEdgeNonMaxSuppression.naive(intensity,direction,output);

			assertEquals(0,output.get(1,1),1e-4f);

			// test no suppression
			setByDirection(intensity,dir,5);
			ImplEdgeNonMaxSuppression.naive(intensity,direction,output);

			assertEquals(intensity.get(1,1),output.get(1,1),1e-4f);
		}
	}

	private void setByDirection( ImageFloat32 img , int dir , float value ) {
		if( dir == 0 ) {
			img.set(1,0,value);
			img.set(1,2,value);
		} else if( dir == 1 ) {
			img.set(0,2,value);
			img.set(2,0,value);
		} else if( dir == 2 ) {
			img.set(0,1,value);
			img.set(2,1,value);
		} else {
			img.set(0,0,value);
			img.set(2,2,value);
		}
	}

	@Test
	public void inner() {
		ImageFloat32 intensity = new ImageFloat32(width,height);
		ImageUInt8 direction = new ImageUInt8(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		ImageTestingOps.randomize(intensity,rand,0,100);
		ImageTestingOps.randomize(direction,rand,0,4);

		GecvTesting.checkSubImage(this,"inner",true,intensity, direction, expected, found);
	}

	public void inner(ImageFloat32 intensity, ImageUInt8 direction, ImageFloat32 expected, ImageFloat32 found) {
		ImplEdgeNonMaxSuppression.naive(intensity,direction,expected);
		ImplEdgeNonMaxSuppression.inner(intensity,direction,found);

		// just test the inside border
		GecvTesting.assertEquals(expected.subimage(1,1,width-1,height-1),
				found.subimage(1,1,width-1,height-1),0,1e-4);
	}

	@Test
	public void border() {
		ImageFloat32 intensity = new ImageFloat32(width,height);
		ImageUInt8 direction = new ImageUInt8(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		ImageTestingOps.randomize(intensity,rand,0,100);
		ImageTestingOps.randomize(direction,rand,0,4);

		GecvTesting.checkSubImage(this,"border",true,intensity, direction, expected, found);
	}

	public void border(ImageFloat32 intensity, ImageUInt8 direction, ImageFloat32 expected, ImageFloat32 found) {
		ImplEdgeNonMaxSuppression.naive(intensity,direction,expected);
		ImplEdgeNonMaxSuppression.border(intensity,direction,found);

		// just test the image border
		GecvTesting.assertEqualsBorder(expected,found,1e-3f,1,1);
	}
}
