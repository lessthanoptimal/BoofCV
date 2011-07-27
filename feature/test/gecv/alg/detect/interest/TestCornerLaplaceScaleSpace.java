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
import gecv.abst.detect.corner.WrapperGradientCornerIntensity;
import gecv.abst.detect.extract.CornerExtractor;
import gecv.abst.detect.extract.FactoryFeatureFromIntensity;
import gecv.abst.filter.ImageFunctionSparse;
import gecv.abst.filter.derivative.FactoryDerivativeSparse;
import gecv.alg.detect.corner.FactoryCornerIntensity;
import gecv.alg.detect.corner.GradientCornerIntensity;
import gecv.core.image.GeneralizedImageOps;
import gecv.struct.gss.FactoryGaussianScaleSpace;
import gecv.struct.gss.GaussianScaleSpace;
import gecv.struct.image.ImageFloat32;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCornerLaplaceScaleSpace {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	int r = 2;

	/**
	 * Very basic test that just checks to see if it can find an obvious feature
	 */
	@Test
	public void test() {
		ImageFloat32 input = new ImageFloat32(width,height);
		GeneralizedImageOps.fillRectangle(input,20,10,10,width,height);

		GaussianScaleSpace<ImageFloat32,ImageFloat32> ss = FactoryGaussianScaleSpace.nocache_F32(3);
		ss.setScales(1,2,3,4);

		CornerExtractor extractor = FactoryFeatureFromIntensity.create(r,5,false,false,false);
		GradientCornerIntensity<ImageFloat32> harris = FactoryCornerIntensity.createHarris(ImageFloat32.class,r,0.04f);
		GeneralFeatureIntensity<ImageFloat32, ImageFloat32> intensity =
				new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(harris);
		GeneralFeatureDetector<ImageFloat32,ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32,ImageFloat32>(intensity,extractor,200);

		ImageFunctionSparse<ImageFloat32> sparseLaplace = FactoryDerivativeSparse.createLaplacian(ImageFloat32.class,null);

		FeatureLaplaceScaleSpace<ImageFloat32,ImageFloat32> alg =
				new FeatureLaplaceScaleSpace<ImageFloat32,ImageFloat32>(detector,sparseLaplace);

		// give it one corner to find
		GeneralizedImageOps.fillRectangle(input,20,10,10,width-10,height-10);
		ss.setImage(input);
		alg.detect(ss);

		List<ScalePoint> found = alg.getInterestPoints();
		assertTrue(found.size()==1);
	}
}
