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

package gecv.alg.detect.interest;

import gecv.abst.detect.corner.GeneralFeatureDetector;
import gecv.abst.detect.corner.GeneralFeatureIntensity;
import gecv.abst.detect.corner.WrapperLaplacianBlobIntensity;
import gecv.abst.detect.extract.FactoryFeatureFromIntensity;
import gecv.abst.detect.extract.FeatureExtractor;
import gecv.alg.detect.corner.HessianBlobIntensity;
import gecv.core.image.GeneralizedImageOps;
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
public abstract class GenericFeatureScaleDetector {

		Random rand = new Random(234);
	int width = 20;
	int height = 30;

	int r = 2;

	/**
	 * Checks to see if features are flushed after multiple calls
	 */
	@Test
	public void checkFlushFeatures() {
		double scales[]=new double[]{1,2,4,8};
		ImageFloat32 input = new ImageFloat32(width,height);
		GeneralizedImageOps.fillRectangle(input,20,10,10,width,height);

		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(r,1,0,false,false,false);
		GeneralFeatureIntensity<ImageFloat32, ImageFloat32> intensity =
				new WrapperLaplacianBlobIntensity<ImageFloat32,ImageFloat32>(HessianBlobIntensity.Type.DETERMINANT,ImageFloat32.class);
		GeneralFeatureDetector<ImageFloat32,ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32,ImageFloat32>(intensity,extractor,200);

		// give it one corner to find
		GeneralizedImageOps.fill(input,50);
		drawCircle(input,10,10,r*2);

		Object alg = createDetector(detector);
		int firstFound = detectFeature(input, scales,alg).size();
		int secondFound = detectFeature(input, scales,alg).size();

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

		FeatureExtractor extractor = FactoryFeatureFromIntensity.create(r,1,0,false,false,false);
		GeneralFeatureIntensity<ImageFloat32, ImageFloat32> intensity =
				new WrapperLaplacianBlobIntensity<ImageFloat32,ImageFloat32>(HessianBlobIntensity.Type.DETERMINANT,ImageFloat32.class);
		GeneralFeatureDetector<ImageFloat32,ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32,ImageFloat32>(intensity,extractor,200);

		// give it one corner to find
		GeneralizedImageOps.fill(input,50);
		drawCircle(input,10,10,r*2);

		Object alg = createDetector(detector);
		List<ScalePoint> found = detectFeature(input,scales,alg);

		assertTrue(found.size()==1);
		ScalePoint p = found.get(0);
		assertEquals(2,p.scale,1e-4);
		assertEquals(10,p.x,r);
		assertEquals(10,p.y,r);
	}

	protected abstract Object createDetector( GeneralFeatureDetector<ImageFloat32, ImageFloat32> detector);

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
