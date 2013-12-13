/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import java.util.ArrayList;
import java.util.List;

/**
 * Example of how to use SURF detector and descriptors in BoofCV. 
 * 
 * @author Peter Abeles
 */
public class ExampleFeatureSurf {

	/**
	 * Use generalized interfaces for working with SURF.  This removes much of the drudgery, but also reduces flexibility
	 * and slightly increases memory and computational requirements.
	 * 
	 *  @param image Input image type. DOES NOT NEED TO BE ImageFloat32, ImageUInt8 works too
	 */
	public static void easy( ImageFloat32 image ) {
		// create the detector and descriptors
		DetectDescribePoint<ImageFloat32,SurfFeature> surf = FactoryDetectDescribe.
				surfStable(new ConfigFastHessian(0, 2, 200, 2, 9, 4, 4), null, null,ImageFloat32.class);

		 // specify the image to process
		surf.detect(image);

		System.out.println("Found Features: "+surf.getNumberOfFeatures());
		System.out.println("First descriptor's first value: "+surf.getDescription(0).value[0]);
	}

	/**
	 * Configured exactly the same as the easy example above, but require a lot more code and a more in depth
	 * understanding of how SURF works and is configured.  Instead of TupleDesc_F64, SurfFeature are computed in
	 * this case.  They are almost the same as TupleDesc_F64, but contain the Laplacian's sign which can be used
	 * to speed up association. That is an example of how using less generalized interfaces can improve performance.
	 * 
	 * @param image Input image type. DOES NOT NEED TO BE ImageFloat32, ImageUInt8 works too
	 */
	public static <II extends ImageSingleBand> void harder( ImageFloat32 image ) {
		// SURF works off of integral images
		Class<II> integralType = GIntegralImageOps.getIntegralType(ImageFloat32.class);
		
		// define the feature detection algorithm
		NonMaxSuppression extractor =
				FactoryFeatureExtractor.nonmax(new ConfigExtract(2, 0, 5, true));
		FastHessianFeatureDetector<II> detector = 
				new FastHessianFeatureDetector<II>(extractor,200,2, 9,4,4);

		// estimate orientation
		OrientationIntegral<II> orientation = 
				FactoryOrientationAlgs.sliding_ii(null, integralType);

		DescribePointSurf<II> descriptor = FactoryDescribePointAlgs.<II>surfStability(null,integralType);
		
		// compute the integral image of 'image'
		II integral = GeneralizedImageOps.createSingleBand(integralType,image.width,image.height);
		GIntegralImageOps.transform(image, integral);

		// detect fast hessian features
		detector.detect(integral);
		// tell algorithms which image to process
		orientation.setImage(integral);
		descriptor.setImage(integral);

		List<ScalePoint> points = detector.getFoundPoints();

		List<SurfFeature> descriptions = new ArrayList<SurfFeature>();

		for( ScalePoint p : points ) {
			// estimate orientation
			orientation.setScale(p.scale);
			double angle = orientation.compute(p.x,p.y);
			
			// extract the SURF description for this region
			SurfFeature desc = descriptor.createDescription();
			descriptor.describe(p.x,p.y,angle,p.scale,desc);

			// save everything for processing later on
			descriptions.add(desc);
		}
		
		System.out.println("Found Features: "+points.size());
		System.out.println("First descriptor's first value: "+descriptions.get(0).value[0]);
	}

	public static void main( String args[] ) {
		
		ImageFloat32 image = UtilImageIO.loadImage("../data/evaluation/particles01.jpg",ImageFloat32.class);
		
		// run each example
		ExampleFeatureSurf.easy(image);
		ExampleFeatureSurf.harder(image);
		
		System.out.println("Done!");
		
	}
}
