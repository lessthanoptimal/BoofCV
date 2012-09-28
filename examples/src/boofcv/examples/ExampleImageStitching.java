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


import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.sfm.robust.DistanceHomographySq;
import boofcv.alg.sfm.robust.GenerateHomographyLinear;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.gui.image.HomographyStitchPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.Ransac;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDescQueue;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.point.Point2D_F64;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * <p> Exampling showing how to combines two images together by finding the best fit image transform with point
 * features.</p>
 * <p>
 * Algorithm Steps:<br>
 * <ol>
 * <li>Detect feature locations</li>
 * <li>Compute feature descriptors</li>
 * <li>Associate features together</li>
 * <li>Use robust fitting to find transform</li>
 * <li>Render combined image</li>
 * </ol>
 * </p>
 *
 * @author Peter Abeles
 */
public class ExampleImageStitching {

	/**
	 * Using abstracted code, find a transform which minimizes the difference between corresponding features
	 * in both images.  This code is completely model independent and is the core algorithms.
	 */
	public static<T extends ImageSingleBand, FD extends TupleDesc> Homography2D_F64
	computeTransform( T imageA , T imageB ,
					  InterestPointDetector<T> detector ,
					  DescribeRegionPoint<T, FD> describe ,
					  GeneralAssociation<FD> associate ,
					  ModelMatcher<Homography2D_F64,AssociatedPair> modelMatcher )
	{
		// see if the detector has everything that the describer needs
		if( describe.requiresOrientation() && !detector.hasOrientation() )
			throw new IllegalArgumentException("Requires orientation be provided.");
		if( describe.requiresScale() && !detector.hasScale() )
			throw new IllegalArgumentException("Requires scale be provided.");

		// get the length of the description
		List<Point2D_F64> pointsA = new ArrayList<Point2D_F64>();
		FastQueue<FD> descA = new TupleDescQueue<FD>(describe,true);
		List<Point2D_F64> pointsB = new ArrayList<Point2D_F64>();
		FastQueue<FD> descB = new TupleDescQueue<FD>(describe,true);

		// extract feature locations and descriptions from each image
		describeImage(imageA, detector, describe, pointsA, descA);
		describeImage(imageB, detector, describe, pointsB, descB);

		// Associate features between the two images
		associate.associate(descA,descB);

		// create a list of AssociatedPairs that tell the model matcher how a feature moved
		FastQueue<AssociatedIndex> matches = associate.getMatches();
		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();

		for( int i = 0; i < matches.size(); i++ ) {
			AssociatedIndex match = matches.get(i);

			Point2D_F64 a = pointsA.get(match.src);
			Point2D_F64 b = pointsB.get(match.dst);

			pairs.add( new AssociatedPair(a,b,false));
		}

		// find the best fit model to describe the change between these images
		if( !modelMatcher.process(pairs) )
			throw new RuntimeException("Model Matcher failed!");

		// return the found image transform
		return modelMatcher.getModel();
	}

	/**
	 * Detects features inside the two images and computes descriptions at those points.
	 */
	private static <T extends ImageSingleBand, FD extends TupleDesc>
	void describeImage(T image,
					   InterestPointDetector<T> detector,
					   DescribeRegionPoint<T,FD> describe,
					   List<Point2D_F64> points,
					   FastQueue<FD> listDescs) {
		detector.detect(image);
		describe.setImage(image);

		listDescs.reset();
		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			// get the feature location info
			Point2D_F64 p = detector.getLocation(i);
			double yaw = detector.getOrientation(i);
			double scale = detector.getScale(i);

			// extract the description and save the results into the provided description
			if( describe.isInBounds(p.x,p.y,yaw,scale) ) {
				describe.process(p.x, p.y, yaw, scale, listDescs.pop());
				points.add(p.copy());
			}
		}
	}

	/**
	 * Given two input images create and display an image where the two have been overlayed on top of each other.
	 */
	public static <T extends ImageSingleBand>
	void stitch( BufferedImage imageA , BufferedImage imageB , Class<T> imageType )
	{
		T inputA = ConvertBufferedImage.convertFromSingle(imageA, null, imageType);
		T inputB = ConvertBufferedImage.convertFromSingle(imageB, null, imageType);

		// Detect using the standard SURF feature descriptor and describer
		InterestPointDetector<T> detector = FactoryInterestPoint.fastHessian(1, 2, 400, 1, 9, 4, 4);
		DescribeRegionPoint<T,SurfFeature> describe = FactoryDescribeRegionPoint.surf(true,imageType);
		ScoreAssociation<SurfFeature> scorer = FactoryAssociation.scoreEuclidean(SurfFeature.class,true);
		GeneralAssociation<SurfFeature> associate = FactoryAssociation.greedy(scorer,2,-1,true);

		// fit the images using a homography.  This works well for rotations and distant objects.
		GenerateHomographyLinear modelFitter = new GenerateHomographyLinear(true);
		DistanceHomographySq distance = new DistanceHomographySq();

		ModelMatcher<Homography2D_F64,AssociatedPair> modelMatcher =
				new Ransac<Homography2D_F64,AssociatedPair>(123,modelFitter,distance,60,9);

		Homography2D_F64 H = computeTransform(inputA, inputB, detector, describe, associate, modelMatcher);

		// draw the results
		HomographyStitchPanel panel = new HomographyStitchPanel(0.5,inputA.width,inputA.height);
		panel.configure(imageA,imageB,H);
		ShowImages.showWindow(panel,"Stitched Images");
	}

	public static void main( String args[] ) {
		BufferedImage imageA,imageB;
		imageA = UtilImageIO.loadImage("../data/evaluation/stitch/mountain_rotate_01.jpg");
		imageB = UtilImageIO.loadImage("../data/evaluation/stitch//mountain_rotate_03.jpg");
		stitch(imageA,imageB, ImageFloat32.class);
		imageA = UtilImageIO.loadImage("../data/evaluation/stitch/kayak_01.jpg");
		imageB = UtilImageIO.loadImage("../data/evaluation/stitch/kayak_03.jpg");
		stitch(imageA,imageB, ImageFloat32.class);
		imageA = UtilImageIO.loadImage("../data/evaluation/scale/rainforest_01.jpg");
		imageB = UtilImageIO.loadImage("../data/evaluation/scale/rainforest_02.jpg");
		stitch(imageA,imageB, ImageFloat32.class);
	}
}
