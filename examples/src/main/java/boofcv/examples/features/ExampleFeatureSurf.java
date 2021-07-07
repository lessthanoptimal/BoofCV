/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.features;

import boofcv.BoofDefaults;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.FactoryDescribeAlgs;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

import java.util.ArrayList;
import java.util.List;

/**
 * Example of how to use SURF detector and descriptors in BoofCV. 
 * 
 * @author Peter Abeles
 */
public class ExampleFeatureSurf {
	/**
	 * Use generalized interfaces for working with SURF. This removes much of the drudgery, but also reduces flexibility
	 * and slightly increases memory and computational requirements.
	 * 
	 *  @param image Input image type. DOES NOT NEED TO BE GrayF32, GrayU8 works too
	 */
	public static void easy( GrayF32 image ) {
		// create the detector and descriptors
		ConfigFastHessian configDetector = new ConfigFastHessian();
		configDetector.extract = new ConfigExtract(2, 0, 5, true);
		configDetector.maxFeaturesPerScale = 200;
		configDetector.initialSampleStep = 2;

		DetectDescribePoint<GrayF32,TupleDesc_F64> surf = FactoryDetectDescribe.
				surfStable(configDetector, null, null,GrayF32.class);

		 // specify the image to process
		surf.detect(image);

		System.out.println("Found Features: "+surf.getNumberOfFeatures());
		System.out.println("First descriptor's first value: "+surf.getDescription(0).data[0]);
	}

	/**
	 * Configured exactly the same as the easy example above, but require a lot more code and a more in depth
	 * understanding of how SURF works and is configured. Each sub-problem which composes "SURF" is now explicitly
	 * created and configured independently. This allows an advance user to tune it for a specific problem.
	 *
	 * @param image Input image type. DOES NOT NEED TO BE GrayF32, GrayU8 works too
	 */
	public static <II extends ImageGray<II>> void harder(GrayF32 image ) {
		// SURF works off of integral images
		Class<II> integralType = GIntegralImageOps.getIntegralType(GrayF32.class);
		
		// define the feature detection algorithm
		ConfigFastHessian config = new ConfigFastHessian();
		config.extract = new ConfigExtract(2, 0, 5, true);
		config.maxFeaturesPerScale = 200;
		config.initialSampleStep = 2;
		FastHessianFeatureDetector<II> detector = FactoryInterestPointAlgs.fastHessian(config);

		// estimate orientation
		OrientationIntegral<II> orientation =  FactoryOrientationAlgs.sliding_ii(null, integralType);

		DescribePointSurf<II> descriptor = FactoryDescribeAlgs.surfStability(null,integralType);
		
		// compute the integral image of 'image'
		II integral = GeneralizedImageOps.createSingleBand(integralType,image.width,image.height);
		GIntegralImageOps.transform(image, integral);

		// detect fast hessian features
		detector.detect(integral);
		// tell algorithms which image to process
		orientation.setImage(integral);
		descriptor.setImage(integral);

		List<ScalePoint> points = detector.getFoundFeatures();

		List<TupleDesc_F64> descriptions = new ArrayList<>();

		for( ScalePoint p : points ) {
			// estimate orientation
			orientation.setObjectRadius( p.scale*BoofDefaults.SURF_SCALE_TO_RADIUS);
			double angle = orientation.compute(p.pixel.x,p.pixel.y);
			
			// extract the SURF description for this region
			TupleDesc_F64 desc = descriptor.createDescription();
			descriptor.describe(p.pixel.x,p.pixel.y,angle,p.scale, true, desc);

			// save everything for processing later on
			descriptions.add(desc);
		}
		
		System.out.println("Found Features: "+points.size());
		System.out.println("First descriptor's first value: "+descriptions.get(0).data[0]);
	}

	public static void main( String[] args ) {
		// Need to turn off concurrency since the order in which feature are returned
		// is not deterministic if turned on
		BoofConcurrency.USE_CONCURRENT = false;

		GrayF32 image = UtilImageIO.loadImage(UtilIO.pathExample("particles01.jpg"), GrayF32.class);
		
		// run each example
		ExampleFeatureSurf.easy(image);
		ExampleFeatureSurf.harder(image);
		
		System.out.println("Done!");
	}
}
