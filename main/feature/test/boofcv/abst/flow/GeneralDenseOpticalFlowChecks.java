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

package boofcv.abst.flow;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class GeneralDenseOpticalFlowChecks<T extends ImageGray>
{
	Random rand = new Random(234);
	Class<T> imageType;

	T orig;
	T shifted;
	ImageFlow found;

	boolean justCorrectSign = false;

	protected GeneralDenseOpticalFlowChecks(Class<T> imageType) {
		this.imageType = imageType;

		orig = GeneralizedImageOps.createSingleBand(imageType,20,25);
		shifted = GeneralizedImageOps.createSingleBand(imageType,20,25);

		found = new ImageFlow(20,25);

		GImageMiscOps.fillUniform(orig,rand,0,256);
	}

	public void setJustCorrectSign(boolean justCorrectSign) {
		this.justCorrectSign = justCorrectSign;
	}


	public void allTests( boolean justCorrectSign ) {
		this.justCorrectSign = justCorrectSign;
		allTests();
	}


	public void allTests() {
		processEdges();
		checkPlanarMotion();
		checkChangeInputSize();
		checkSubImage();
	}

	public abstract DenseOpticalFlow<T> createAlg( Class<T> imageType );

	/**
	 * Makes sure it attempts to compute flow through out the whole image.  Specially checks the image border
	 * to see if those are skipped
	 */
	@Test
	public void processEdges() {
		shift(orig,1,0,shifted);

		DenseOpticalFlow<T> alg = createAlg( imageType );

		found.invalidateAll();
		alg.process(orig,shifted,found);

		int count0 = 0, count1 = 0;
		for( int x = 0; x < shifted.width; x++ ) {
			if( found.get(x,0).isValid() )
				count0++;
			if( found.get(x,found.height-2).isValid() )
				count1++;
		}

		assertTrue(count0>=found.width/3);
		assertTrue(count1>=found.width/3);


		// process it again so that there should be an obvious solution along the left and right sides
		found.invalidateAll();
		shift(orig, 0, 1, shifted);
		alg.process(orig,shifted,found);

		int count2 = 0, count3 = 0;
		for( int y = 0; y < shifted.height; y++ ) {
			if( found.get(0,y).isValid() )
				count2++;
			if( found.get(found.width-2,y).isValid() )
				count3++;
		}

		assertTrue(count2>=found.height/3);
		assertTrue(count3>=found.height/3);
	}

	/**
	 * Very simple test where every pixel moves at the same speed along x and or y direction
	 */
	@Test
	public void checkPlanarMotion() {

		for( int dy = -1; dy <= 1; dy++ ){
			for( int dx = -1; dx <= 1; dx++ ){
				DenseOpticalFlow<T> alg = createAlg( imageType );
				shift(orig,dx,dy,shifted);

				found.invalidateAll();
				alg.process(orig,shifted,found);

				ImageFlow.D flow = found.get(10,10);
				assertTrue(flow.isValid());
				if( justCorrectSign ) {
					// if the two flows are in agreement then sum will be positive
					float sum = 0;
					for( int y = 0; y < found.height; y++ ) {
						for (int x = 0; x < found.width; x++) {
							flow = found.get(x,y);
							sum += flow.x*dx;
							sum += flow.y*dy;
						}
					}
					assertTrue( sum >= 0 );
				} else {
					assertEquals(dx, flow.x, 0.2);
					assertEquals(dy, flow.y, 0.2);
				}
			}
		}
	}

	/**
	 * Does it handle the input image size being changed after the first image?
	 */
	@Test
	public void checkChangeInputSize() {
		DenseOpticalFlow<T> alg = createAlg( imageType );

		alg.process(orig,shifted,found);

		T larger0 = GeneralizedImageOps.createSingleBand(imageType,40,35);
		T larger1 = GeneralizedImageOps.createSingleBand(imageType,40,35);

		// if it doesn't blow up it worked
		alg.process(larger0,larger1,new ImageFlow(40,35));
	}

	@Test
	public void checkSubImage() {
		DenseOpticalFlow<T> alg = createAlg( imageType );

		shift(orig,1,-1,shifted);
		alg.process(orig,shifted,found);

		// should produce identical solution
		T subOrig = BoofTesting.createSubImageOf(orig);
		T subShifted = BoofTesting.createSubImageOf(shifted);
		ImageFlow found2 = new ImageFlow(found.width,found.height);
		alg.process(subOrig,subShifted,found2);

		for( int y = 0; y < found.height; y++ ) {
			for( int x = 0; x < found.width; x++ ){
				ImageFlow.D a = found.get(x,y);
				ImageFlow.D b = found2.get(x,y);

				if( a.isValid() ) {
					assertTrue( a.x == b.x );
					assertTrue( a.y == b.y );
				} else {
					assertFalse(b.isValid());
				}
			}
		}
	}

	private void shift( T input , int dx , int dy , T output ) {

		int w = input.width;
		int h = input.height;

		if( dx >= 0 ){
			output.subimage(dx,0,w,h).setTo(input.subimage(0,0,w-dx,h));
			output.subimage(0,0,dx,h).setTo(input.subimage(w-dx,0,w,h));
		} else {
			output.subimage(0,0,w+dx,h).setTo(input.subimage(-dx,0,w,h));
			output.subimage(w+dx,0,w,h).setTo(input.subimage(0,0,-dx,h));
		}

		T tmp = (T)output.clone();

		if( dy >= 0 ){
			output.subimage(0,dy,w,h).setTo(tmp.subimage(0,0,w,h-dy));
			output.subimage(0,0,w,dy).setTo(tmp.subimage(0,h-dy,w,h));
		} else {
			output.subimage(0,0,w,h+dy).setTo(tmp.subimage(0,-dy,w,h));
			output.subimage(0,h+dy,w,h).setTo(tmp.subimage(0,0,w,-dy));
		}
	}
}
