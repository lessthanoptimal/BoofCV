/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.examples;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

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
	 * and slightly increases memory and computational requirements.  For example, the integral image is computed twice.
	 * 
	 *  @param image Input image type. DOES NOT NEED TO BE ImageFloat32, ImageUInt8 works too
	 */
	public static void easy( ImageFloat32 image ) {
		// create the detector and descriptors
		InterestPointDetector<ImageFloat32> detector = FactoryInterestPoint.fastHessian(0, 2, 200, 2, 9, 4, 4);
		// BoofCV has two SURF implementations.  surfm() = slower, but more accurate.  surf() = faster and less accurate
		DescribeRegionPoint<ImageFloat32> descriptor = FactoryDescribeRegionPoint.surfm(true,ImageFloat32.class);
		
		// just pointing out that orientation does not need to be passed into the descriptor
		if( descriptor.requiresOrientation() )
			throw new RuntimeException("SURF should compute orientation itself!");
		
		// detect interest points
		detector.detect(image);
		 // specify the image to process
		descriptor.setImage(image);
		
		List<Point2D_F64> locations = new ArrayList<Point2D_F64>();
		List<TupleDesc_F64> descriptions = new ArrayList<TupleDesc_F64>();
		
		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			// information about hte detected interest point
			Point2D_F64 p = detector.getLocation(i);
			double scale = detector.getScale(i);
			
			// extract the SURF description for this region
			TupleDesc_F64 desc = descriptor.process(p.x,p.y,0,scale,null);
			
			// save everything for processing later on
			descriptions.add(desc);
			locations.add(p);
		}

		System.out.println("Found Features: "+locations.size());
		System.out.println("First descriptor's first value: "+descriptions.get(0).value[0]);
	}

	/**
	 * Configured exactly the same as the easy example above, but require a lot more code and a more in depth understanding
	 * of how SURF works and is configured.  Instead of TupleDesc_F64, SurfFeature are computed in this case.  They are
	 * almost the same as TupleDesc_F64, but contain the Laplacian's sign which can be used to speed up association.
	 * That is an example of how using less generalized interfaces can improve performance.
	 * 
	 * @param image Input image type. DOES NOT NEED TO BE ImageFloat32, ImageUInt8 works too
	 */
	public static <II extends ImageSingleBand> void harder( ImageFloat32 image ) {
		// SURF works off of integral images
		Class<II> integralType = GIntegralImageOps.getIntegralType(ImageFloat32.class);
		
		// define the feature detection algorithm
		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(2, 0, 5, false, true);
		FastHessianFeatureDetector<II> detector = 
				new FastHessianFeatureDetector<II>(extractor,200,2, 9,4,4);

		// estimate orientation
		OrientationIntegral<II> orientation = 
				FactoryOrientationAlgs.sliding_ii(0.65, Math.PI / 3.0, 8, -1, 6, integralType);

		DescribePointSurf<II> descriptor = FactoryDescribePointAlgs.<II>msurf(integralType);
		
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
			SurfFeature desc = descriptor.describe(p.x,p.y,p.scale,angle,null);

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
