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

package boofcv.alg.feature.detect.edge.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import boofcv.testing.BoofTesting;
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
	public void naive4() {
		GrayF32 intensity = new GrayF32(3,3);
		GrayF32 output = new GrayF32(3,3);
		GrayS8 direction = new GrayS8(3,3);

		// test it against simple positive and negative cases
		for( int dir = -1; dir < 3; dir++ ) {
			direction.set(1,1,dir);

			GImageMiscOps.fill(intensity, 0);
			intensity.set(1,1,10);

			// test suppress
			setByDirection4(intensity,dir,15);
			ImplEdgeNonMaxSuppression.naive4(intensity,direction,output);

			assertEquals(0,output.get(1,1),1e-4f);

			// test no suppression
			setByDirection4(intensity,dir,5);
			ImplEdgeNonMaxSuppression.naive4(intensity,direction,output);

			assertEquals(intensity.get(1,1),output.get(1,1),1e-4f);
		}
	}

	private void setByDirection4(GrayF32 img , int dir , float value ) {
		if( dir == 0 ) {
			img.set(0,1,value);
			img.set(2,1,value);
		} else if( dir == 1 ) {
			img.set(2,2,value);
			img.set(0,0,value);
		} else if( dir == 2 ) {
			img.set(1,0,value);
			img.set(1,2,value);
		} else {
			img.set(2,0,value);
			img.set(0,2,value);
		}
	}

	/**
	 * Make sure it suppresses values of equal intensity
	 */
	@Test
	public void naive4_equal() {
		GrayF32 intensity = new GrayF32(3,3);
		GrayF32 output = new GrayF32(3,3);
		GrayS8 direction = new GrayS8(3,3);

		GImageMiscOps.fill(intensity, 2);

		ImplEdgeNonMaxSuppression.naive4(intensity,direction,output);

		for( int i = 0; i < output.data.length; i++ )
			assertEquals(2,output.data[i],1e-4f);
	}

	@Test
	public void naive8() {
		GrayF32 intensity = new GrayF32(3,3);
		GrayF32 output = new GrayF32(3,3);
		GrayS8 direction = new GrayS8(3,3);

		// test it against simple positive and negative cases
		for( int dir = -3; dir < 5; dir++ ) {
			direction.set(1,1,dir);

			GImageMiscOps.fill(intensity,0);
			intensity.set(1,1,10);

			// test suppress
			setByDirection8(intensity,dir,15);
			ImplEdgeNonMaxSuppression.naive8(intensity,direction,output);

			assertEquals(0,output.get(1,1),1e-4f);

			// test no suppression
			setByDirection8(intensity,dir,5);
			ImplEdgeNonMaxSuppression.naive8(intensity,direction,output);

			assertEquals(intensity.get(1,1),output.get(1,1),1e-4f);
		}
	}

	private void setByDirection8(GrayF32 img , int dir , float value ) {
		if( dir == 0 || dir == 4) {
			img.set(0,1,value);
			img.set(2,1,value);
		} else if( dir == 1 || dir == -3) {
			img.set(2,2,value);
			img.set(0,0,value);
		} else if( dir == 2 || dir == -2) {
			img.set(1,0,value);
			img.set(1,2,value);
		} else {
			img.set(2,0,value);
			img.set(0,2,value);
		}
	}

	/**
	 * Make sure it suppresses values of equal intensity
	 */
	@Test
	public void naive8_equal() {
		GrayF32 intensity = new GrayF32(3,3);
		GrayF32 output = new GrayF32(3,3);
		GrayS8 direction = new GrayS8(3,3);

		GImageMiscOps.fill(intensity, 2);

		ImplEdgeNonMaxSuppression.naive8(intensity, direction, output);

		for( int i = 0; i < output.data.length; i++ )
			assertEquals(2,output.data[i],1e-4f);
	}

	@Test
	public void inner4() {
		GrayF32 intensity = new GrayF32(width,height);
		GrayS8 direction = new GrayS8(width,height);
		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		BoofTesting.checkSubImage(this,"inner4",true,intensity, direction, expected, found);
	}

	public void inner4(GrayF32 intensity, GrayS8 direction, GrayF32 expected, GrayF32 found) {

		ImageMiscOps.fillUniform(intensity, rand, 0, 100);
		ImageMiscOps.fillUniform(direction, rand, -1, 3);

		ImplEdgeNonMaxSuppression.naive4(intensity,direction,expected);
		ImplEdgeNonMaxSuppression.inner4(intensity,direction,found);

		// just test the inside border
		BoofTesting.assertEquals(expected.subimage(1,1,width-1,height-1, null),
				found.subimage(1,1,width-1,height-1, null), 1e-4);

		// make sure it handles the constant intensity case
		ImageMiscOps.fill(intensity,2);

		ImplEdgeNonMaxSuppression.naive4(intensity,direction,expected);
		ImplEdgeNonMaxSuppression.inner4(intensity,direction,found);

		BoofTesting.assertEquals(expected.subimage(1,1,width-1,height-1, null),
				found.subimage(1,1,width-1,height-1, null), 1e-4);
	}

	@Test
	public void inner8() {
		GrayF32 intensity = new GrayF32(width,height);
		GrayS8 direction = new GrayS8(width,height);
		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		BoofTesting.checkSubImage(this,"inner8",true,intensity, direction, expected, found);
	}

	public void inner8(GrayF32 intensity, GrayS8 direction, GrayF32 expected, GrayF32 found) {
		ImageMiscOps.fillUniform(intensity, rand, 0, 100);
		ImageMiscOps.fillUniform(direction, rand, -3, 5);

		ImplEdgeNonMaxSuppression.naive8(intensity,direction,expected);
		ImplEdgeNonMaxSuppression.inner8(intensity,direction,found);

		// just test the inside border
		BoofTesting.assertEquals(expected.subimage(1,1,width-1,height-1, null),
				found.subimage(1,1,width-1,height-1, null), 1e-4);

		// make sure it handles the constant intensity case
		ImageMiscOps.fill(intensity,2);

		ImplEdgeNonMaxSuppression.naive8(intensity, direction, expected);
		ImplEdgeNonMaxSuppression.inner8(intensity, direction, found);

		BoofTesting.assertEquals(expected.subimage(1,1,width-1,height-1, null),
				found.subimage(1,1,width-1,height-1, null), 1e-4);
	}

	@Test
	public void border4() {
		GrayF32 intensity = new GrayF32(width,height);
		GrayS8 direction = new GrayS8(width,height);
		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		BoofTesting.checkSubImage(this,"border4",true,intensity, direction, expected, found);
	}

	public void border4(GrayF32 intensity, GrayS8 direction, GrayF32 expected, GrayF32 found) {

		// just test the image border
		ImageMiscOps.fillUniform(intensity, rand, 0, 100);
		ImageMiscOps.fillUniform(direction, rand, -1, 3);

		ImplEdgeNonMaxSuppression.naive4(intensity,direction,expected);
		ImplEdgeNonMaxSuppression.border4(intensity,direction,found);

		BoofTesting.assertEqualsBorder(expected,found,1e-3f,1,1);

		// make sure it handles the constant intensity case
		ImageMiscOps.fill(intensity,2);

		ImplEdgeNonMaxSuppression.naive4(intensity,direction,expected);
		ImplEdgeNonMaxSuppression.border4(intensity,direction,found);

		BoofTesting.assertEqualsBorder(expected,found,1e-3f,1,1);

	}

@Test
	public void border8() {
		GrayF32 intensity = new GrayF32(width,height);
		GrayS8 direction = new GrayS8(width,height);
		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		BoofTesting.checkSubImage(this,"border8",true,intensity, direction, expected, found);
	}

	public void border8(GrayF32 intensity, GrayS8 direction, GrayF32 expected, GrayF32 found) {
		// just test the image border
		ImageMiscOps.fillUniform(intensity, rand, 0, 100);
		ImageMiscOps.fillUniform(direction, rand, -3, 5);

		ImplEdgeNonMaxSuppression.naive8(intensity,direction,expected);
		ImplEdgeNonMaxSuppression.border8(intensity,direction,found);

		BoofTesting.assertEqualsBorder(expected,found,1e-3f,1,1);

		// make sure it handles the constant intensity case
		ImageMiscOps.fill(intensity,2);

		ImplEdgeNonMaxSuppression.naive8(intensity, direction, expected);
		ImplEdgeNonMaxSuppression.border8(intensity, direction, found);

		BoofTesting.assertEqualsBorder(expected,found,1e-3f,1,1);
	}
}
