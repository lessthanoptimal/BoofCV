/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow.impl;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestImplImageDifference {

	Random rand = new Random(234);

	@Test
	public void inner4_border4_F32() {
		ImageFloat32 imageA = new ImageFloat32(20,30);
		ImageFloat32 imageB = new ImageFloat32(20,30);
		ImageFloat32 found = new ImageFloat32(20,30);
		ImageFloat32 expected = new ImageFloat32(20,30);


		ImageMiscOps.fillUniform(imageA, rand, 0, 255);
		ImageMiscOps.fillUniform(imageB, rand, 0, 255);

		for( int y = 0; y < imageA.height; y++ ) {
			for (int x = 0; x < imageA.width; x++) {
				expected.set(x,y, diff4(imageA,imageB,x,y));
			}
		}

		ImplImageDifference.border4(imageA,imageB,found);
		ImplImageDifference.inner4(imageA, imageB, found);

		BoofTesting.assertEquals(expected, found, 1e-4);
	}

	public float diff4( ImageFloat32 imageA , ImageFloat32 imageB , int x , int y ) {

		float p0 = get(imageA,imageB,x,y);
		float p1 = get(imageA,imageB,x+1,y);
		float p2 = get(imageA,imageB,x-1,y);
		float p3 = get(imageA,imageB,x,y+1);
		float p4 = get(imageA,imageB,x,y-1);

		return (p0+p1+p2+p3+p4)/5;
	}

	private float get( ImageFloat32 imageA , ImageFloat32 imageB , int x , int y ) {
		if( x < 0 ) x = 0;
		else if( x >= imageA.width ) x = imageA.width-1;

		if( y < 0 ) y = 0;
		else if( y >= imageA.height ) y = imageA.height-1;

		return imageB.get(x,y) - imageA.get(x,y);
	}

	@Test
	public void inner4_border4_U8() {
		ImageUInt8 imageA = new ImageUInt8(20,30);
		ImageUInt8 imageB = new ImageUInt8(20,30);
		ImageSInt16 found = new ImageSInt16(20,30);
		ImageSInt16 expected = new ImageSInt16(20,30);


		ImageMiscOps.fillUniform(imageA, rand, 0, 255);
		ImageMiscOps.fillUniform(imageB, rand, 0, 255);

		for( int y = 0; y < imageA.height; y++ ) {
			for (int x = 0; x < imageA.width; x++) {
				expected.set(x,y, diff4(imageA,imageB,x,y));
			}
		}

		ImplImageDifference.border4(imageA,imageB,found);
		ImplImageDifference.inner4(imageA, imageB, found);

		BoofTesting.assertEquals(expected, found, 1e-4);
	}

	public int diff4( ImageUInt8 imageA , ImageUInt8 imageB , int x , int y ) {

		int p0 = get(imageA,imageB,x,y);
		int p1 = get(imageA,imageB,x+1,y);
		int p2 = get(imageA,imageB,x-1,y);
		int p3 = get(imageA,imageB,x,y+1);
		int p4 = get(imageA,imageB,x,y-1);

		return (p0+p1+p2+p3+p4)/5;
	}

	private int get( ImageUInt8 imageA , ImageUInt8 imageB , int x , int y ) {
		if( x < 0 ) x = 0;
		else if( x >= imageA.width ) x = imageA.width-1;

		if( y < 0 ) y = 0;
		else if( y >= imageA.height ) y = imageA.height-1;

		return imageB.get(x,y) - imageA.get(x,y);
	}

}
