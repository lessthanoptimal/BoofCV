/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.geometry;


import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformHomography_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
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
	public static<T extends ImageGray<T>, FD extends TupleDesc> Homography2D_F64
	computeTransform( T imageA , T imageB ,
					  DetectDescribePoint<T,FD> detDesc ,
					  AssociateDescription<FD> associate ,
					  ModelMatcher<Homography2D_F64,AssociatedPair> modelMatcher )
	{
		// get the length of the description
		List<Point2D_F64> pointsA = new ArrayList<>();
		FastQueue<FD> descA = UtilFeature.createQueue(detDesc,100);
		List<Point2D_F64> pointsB = new ArrayList<>();
		FastQueue<FD> descB = UtilFeature.createQueue(detDesc,100);

		// extract feature locations and descriptions from each image
		describeImage(imageA, detDesc, pointsA, descA);
		describeImage(imageB, detDesc, pointsB, descB);

		// Associate features between the two images
		associate.setSource(descA);
		associate.setDestination(descB);
		associate.associate();

		// create a list of AssociatedPairs that tell the model matcher how a feature moved
		FastQueue<AssociatedIndex> matches = associate.getMatches();
		List<AssociatedPair> pairs = new ArrayList<>();

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
		return modelMatcher.getModelParameters().copy();
	}

	/**
	 * Detects features inside the two images and computes descriptions at those points.
	 */
	private static <T extends ImageGray<T>, FD extends TupleDesc>
	void describeImage(T image,
					   DetectDescribePoint<T,FD> detDesc,
					   List<Point2D_F64> points,
					   FastQueue<FD> listDescs) {
		detDesc.detect(image);

		listDescs.reset();
		for( int i = 0; i < detDesc.getNumberOfFeatures(); i++ ) {
			points.add( detDesc.getLocation(i).copy() );
			listDescs.grow().setTo(detDesc.getDescription(i));
		}
	}

	/**
	 * Given two input images create and display an image where the two have been overlayed on top of each other.
	 */
	public static <T extends ImageGray<T>>
	void stitch( BufferedImage imageA , BufferedImage imageB , Class<T> imageType )
	{
		T inputA = ConvertBufferedImage.convertFromSingle(imageA, null, imageType);
		T inputB = ConvertBufferedImage.convertFromSingle(imageB, null, imageType);

		// Detect using the standard SURF feature descriptor and describer
		DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(
				new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null,null, imageType);
		ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class,true);
		AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer,2,true);

		// fit the images using a homography.  This works well for rotations and distant objects.
		ModelMatcher<Homography2D_F64,AssociatedPair> modelMatcher =
				FactoryMultiViewRobust.homographyRansac(null,new ConfigRansac(60,3));

		Homography2D_F64 H = computeTransform(inputA, inputB, detDesc, associate, modelMatcher);

		renderStitching(imageA,imageB,H);
	}

	/**
	 * Renders and displays the stitched together images
	 */
	public static void renderStitching( BufferedImage imageA, BufferedImage imageB ,
										Homography2D_F64 fromAtoB )
	{
		// specify size of output image
		double scale = 0.5;

		// Convert into a BoofCV color format
		Planar<GrayF32> colorA =
				ConvertBufferedImage.convertFromPlanar(imageA, null, true, GrayF32.class);
		Planar<GrayF32> colorB =
				ConvertBufferedImage.convertFromPlanar(imageB, null,true, GrayF32.class);

		// Where the output images are rendered into
		Planar<GrayF32> work = colorA.createSameShape();

		// Adjust the transform so that the whole image can appear inside of it
		Homography2D_F64 fromAToWork = new Homography2D_F64(scale,0,colorA.width/4,0,scale,colorA.height/4,0,0,1);
		Homography2D_F64 fromWorkToA = fromAToWork.invert(null);

		// Used to render the results onto an image
		PixelTransformHomography_F32 model = new PixelTransformHomography_F32();
		InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(GrayF32.class, BorderType.ZERO);
		ImageDistort<Planar<GrayF32>,Planar<GrayF32>> distort =
				DistortSupport.createDistortPL(GrayF32.class, model, interp, false);
		distort.setRenderAll(false);

		// Render first image
		model.set(fromWorkToA);
		distort.apply(colorA,work);

		// Render second image
		Homography2D_F64 fromWorkToB = fromWorkToA.concat(fromAtoB,null);
		model.set(fromWorkToB);
		distort.apply(colorB,work);

		// Convert the rendered image into a BufferedImage
		BufferedImage output = new BufferedImage(work.width,work.height,imageA.getType());
		ConvertBufferedImage.convertTo(work,output,true);

		Graphics2D g2 = output.createGraphics();

		// draw lines around the distorted image to make it easier to see
		Homography2D_F64 fromBtoWork = fromWorkToB.invert(null);
		Point2D_I32 corners[] = new Point2D_I32[4];
		corners[0] = renderPoint(0,0,fromBtoWork);
		corners[1] = renderPoint(colorB.width,0,fromBtoWork);
		corners[2] = renderPoint(colorB.width,colorB.height,fromBtoWork);
		corners[3] = renderPoint(0,colorB.height,fromBtoWork);

		g2.setColor(Color.ORANGE);
		g2.setStroke(new BasicStroke(4));
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.drawLine(corners[0].x,corners[0].y,corners[1].x,corners[1].y);
		g2.drawLine(corners[1].x,corners[1].y,corners[2].x,corners[2].y);
		g2.drawLine(corners[2].x,corners[2].y,corners[3].x,corners[3].y);
		g2.drawLine(corners[3].x,corners[3].y,corners[0].x,corners[0].y);

		ShowImages.showWindow(output,"Stitched Images", true);
	}

	private static Point2D_I32 renderPoint( int x0 , int y0 , Homography2D_F64 fromBtoWork )
	{
		Point2D_F64 result = new Point2D_F64();
		HomographyPointOps_F64.transform(fromBtoWork, new Point2D_F64(x0, y0), result);
		return new Point2D_I32((int)result.x,(int)result.y);
	}

	public static void main( String args[] ) {
		BufferedImage imageA,imageB;
		imageA = UtilImageIO.loadImage(UtilIO.pathExample("stitch/mountain_rotate_01.jpg"));
		imageB = UtilImageIO.loadImage(UtilIO.pathExample("stitch/mountain_rotate_03.jpg"));
		stitch(imageA,imageB, GrayF32.class);
		imageA = UtilImageIO.loadImage(UtilIO.pathExample("stitch/kayak_01.jpg"));
		imageB = UtilImageIO.loadImage(UtilIO.pathExample("stitch/kayak_03.jpg"));
		stitch(imageA,imageB, GrayF32.class);
		imageA = UtilImageIO.loadImage(UtilIO.pathExample("scale/rainforest_01.jpg"));
		imageB = UtilImageIO.loadImage(UtilIO.pathExample("scale/rainforest_02.jpg"));
		stitch(imageA,imageB, GrayF32.class);
	}
}
