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

package gecv.alg.feature.detect.interest;

import gecv.core.image.GeneralizedImageOps;
import gecv.struct.feature.ScalePoint;
import gecv.struct.image.ImageFloat32;
import jgrl.geometry.UtilPoint2D_I32;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public abstract class GenericFeatureDetector {
	Random rand = new Random(234);

	int width = 50;
	int height = 60;

	int r = 2;

	double scaleTolerance = 1e-4;

	/**
	 * If the maximum number of features is set to a negative number then
	 * it should return the maximum number of features possible
	 */
	@Test
	public void checkNegativeMaxFeatures() {
		double scales[]=new double[]{1,2,4,8};
		ImageFloat32 input = new ImageFloat32(width,height);
		GeneralizedImageOps.randomize(input,rand,0,100);

		// limit it to "one" feature
		Object alg = createDetector(1);
		int firstFound = detectFeature(input, scales,alg).size();

		// tell it to return everything it can find
		 alg = createDetector(0);
		int secondFound = detectFeature(input, scales,alg).size();

		assertTrue(secondFound>firstFound);
	}

	/**
	 * Checks to see if features are flushed after multiple calls
	 */
	@Test
	public void checkFlushFeatures() {
		double scales[]=new double[]{1,2,4,8};
		ImageFloat32 input = new ImageFloat32(width,height);
		GeneralizedImageOps.fillRectangle(input,20,10,10,width,height);

		// give it one corner to find
		GeneralizedImageOps.fill(input,50);
		drawCircle(input,10,10,r*2);

		Object alg = createDetector(50);
		int firstFound = detectFeature(input, scales,alg).size();
		int secondFound = detectFeature(input, scales,alg).size();

		// make sure at least one feature was found
		assertTrue(firstFound>0);
		// if features are not flushed then the secondFound should be twice as large
		assertEquals(firstFound,secondFound);
	}

	/**
	 * Very basic test that just checks to see if it can find an obvious circular feature
	 */
	@Test
	public void basicDetect() {
		double scales[]=new double[]{1,2,4,8};
		ImageFloat32 input = new ImageFloat32(width,height);
		GeneralizedImageOps.fillRectangle(input,20,10,10,width,height);


		// give it one corner to find
		GeneralizedImageOps.fill(input,50);
		drawCircle(input,10,10,r*2);

		Object alg = createDetector(50);
		List<ScalePoint> found = detectFeature(input,scales,alg);

		assertTrue(found.size()>=1);
		
		// look for at least one good match
		boolean foundMatch = false;
		for( ScalePoint p : found ) {
			if( Math.abs(2-p.scale) > scaleTolerance )
				continue;
			if( Math.abs(10-p.x) > r )
				continue;
			if( Math.abs(10-p.y) > r )
				continue;

			foundMatch = true;
			break;
		}
		assertTrue(foundMatch);
	}

	protected abstract Object createDetector( int maxFeatures );

	protected abstract List<ScalePoint> detectFeature(ImageFloat32 input, double[] scales, Object detector);

	private void drawCircle( ImageFloat32 img , int c_x , int c_y , double r ) {

		for( int y = 0; y < img.height; y++ ) {
			for( int x = 0; x < img.width; x++ ) {
				double d = UtilPoint2D_I32.distance(x,y,c_x,c_y);
				if( d <= r ) {
					img.set(x,y,0);
				}
			}
		}
	}
}
