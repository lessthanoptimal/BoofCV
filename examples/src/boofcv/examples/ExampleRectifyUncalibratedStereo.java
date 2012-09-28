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

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyFundamental;
import boofcv.alg.sfm.robust.DistanceAffine2D;
import boofcv.alg.sfm.robust.GenerateAffine2D;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.image.UtilImageIO;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.Ransac;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.affine.Affine2D_F64;
import org.ejml.data.DenseMatrix64F;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Given two uncalibrated stereo images (stereo baseline is unknown) rectify the images by aligning the images along
 * the y-axis.  The uncalibrated case is difficult to apply in practice because it requires an accurate fundamental
 * matrix estimate.  Any outliers or other errors will throw the estimate off by a significant amount. Note
 * how rectification is off by several pixels even in this example.
 * </p>
 *
 * <p>
 * In the example below a hack is used to remove the last few outliers, applying crude affine constraint.
 * The affine constraint works moderately well in this example because the observed location of points after
 * the true rigid body motion has applied is within several pixels of the affine model.
 * </p>
 *
 * <p>
 * Check list for correctly applying uncalibrated rectification:
 * <ul>
 * <li>Must remove all incorrect associated pairs</li>
 * <ul>
 *   <li>Do not rely on the epipolar constraint alone to remove noise</li>
 *   <li>Use a robust estimation algorithm to compute F (e.g. RANSAC).</li>
 *   <li>Remove lens distortion to improve accuracy</li>
 * </ul>
 * <li>Curse CV books for not mentioning these important problems and making it sound trivial</li>
 * <ul>
 * </p>
 *
 * @author Peter Abeles
 */
public class ExampleRectifyUncalibratedStereo {

	/**
	 * Rectifies the image using the provided fundamental matrix.  Both the fundamental matrix
	 * and set of inliers need to be accurate.  Small errors will cause large distortions.
	 *
	 * @param F Fundamental matrix
	 * @param inliers Set of associated pairs between the two images.
	 * @param origLeft Original input image.  Used for output purposes.
	 * @param origRight Original input image.  Used for output purposes.
	 */
	public static void rectify( DenseMatrix64F F , List<AssociatedPair> inliers ,
								BufferedImage origLeft , BufferedImage origRight ) {
		// distorted images
		MultiSpectral<ImageFloat32> distLeft = ConvertBufferedImage.convertFromMulti(origLeft, null, ImageFloat32.class);
		MultiSpectral<ImageFloat32> distRight = ConvertBufferedImage.convertFromMulti(origRight, null, ImageFloat32.class);

		// storage for rectified images
		MultiSpectral<ImageFloat32> rectLeft = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				distLeft.getWidth(),distLeft.getHeight(),distLeft.getNumBands());
		MultiSpectral<ImageFloat32> rectRight = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				distRight.getWidth(),distRight.getHeight(),distRight.getNumBands());

		// Compute rectification
		RectifyFundamental rectifyAlg = RectifyImageOps.createUncalibrated();

		rectifyAlg.process(F,inliers,origLeft.getWidth(),origLeft.getHeight());

		// rectification matrix for each image
		DenseMatrix64F rect1 = rectifyAlg.getRect1();
		DenseMatrix64F rect2 = rectifyAlg.getRect2();

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.fullViewLeft(origLeft.getWidth(),origLeft.getHeight(), rect1, rect2 );
//		RectifyImageOps.allInsideLeft(origLeft.getWidth(),origLeft.getHeight(), rect1, rect2 );

		// undistorted and rectify images
		ImageDistort<ImageFloat32> imageDistortLeft =
				RectifyImageOps.rectifyImage(rect1,ImageFloat32.class);
		ImageDistort<ImageFloat32> imageDistortRight =
				RectifyImageOps.rectifyImage(rect2,ImageFloat32.class);

		DistortImageOps.distortMS(distLeft, rectLeft, imageDistortLeft);
		DistortImageOps.distortMS(distRight, rectRight, imageDistortRight);

		// convert for output
		BufferedImage outLeft = ConvertBufferedImage.convertTo(rectLeft,null);
		BufferedImage outRight = ConvertBufferedImage.convertTo(rectRight, null);

		// show results and draw a horizontal line where the user clicks to see rectification easier
		ShowImages.showWindow(new RectifiedPairPanel(true, outLeft,outRight),"Rectified");
	}

	/**
	 * After the epipolar constraint is used to remove outliers, many still remain.  Here it
	 * is assumed that there is only a small change between feature locations and any that
	 * are far away will be pruned.
	 */
	public static List<AssociatedPair> pruneWithAffine( List<AssociatedPair> pairs ) {

		// use a fairly crude threshold since its not really an affine scene
		double threshold = 5;

		GenerateAffine2D modelFitter = new GenerateAffine2D();
		DistanceAffine2D distance = new DistanceAffine2D();
		int minSamples = modelFitter.getMinimumPoints();
		ModelMatcher<Affine2D_F64,AssociatedPair> modelMatcher =
				new Ransac<Affine2D_F64,AssociatedPair>(
						123,modelFitter,distance,200,threshold);

		if( !modelMatcher.process(pairs) )
			throw new RuntimeException("Prune affine failed");

		return modelMatcher.getMatchSet();
	}

	public static void main( String args[] ) {

		// load images with lens distortion removed
		String dir = "../data/evaluation/structure/";
		BufferedImage imageA = UtilImageIO.loadImage(dir + "undist_cyto_01.jpg");
		BufferedImage imageB = UtilImageIO.loadImage(dir + "undist_cyto_02.jpg");

		// Find a set of point feature matches
		List<AssociatedPair> matches = ExampleFundamentalMatrix.computeMatches(imageA,imageB);

		// Prune matches using the epipolar constraint
		List<AssociatedPair> inliers = new ArrayList<AssociatedPair>();
		ExampleFundamentalMatrix.robustFundamental(matches, inliers);

		// Remove additional outliers by assuming a small change between the images
		inliers = pruneWithAffine(inliers);

		// recompute F using the remaining features pairs
		DenseMatrix64F F = ExampleFundamentalMatrix.simpleFundamental(inliers);

		// display the inlier matches found using the robust estimator
		AssociationPanel panel = new AssociationPanel(20);
		panel.setAssociation(inliers);
		panel.setImages(imageA,imageB);

		ShowImages.showWindow(panel, "Inlier Pairs");

		rectify(F,inliers,imageA,imageB);
	}
}
