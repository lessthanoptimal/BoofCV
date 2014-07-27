/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.stereo;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyFundamental;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import org.ejml.data.DenseMatrix64F;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * In this example, two images are rectified using a Fundamental matrix.  This is known as uncalibrated stereo
 * rectification since the camera's intrinsic parameters and stereo baseline are not known.  The fundamental
 * matrix is computed by matching image features, followed by robust estimation using RANSAC to remove outliers.
 * </p>
 *
 * <p>
 * Lens distortion has been removed to increase its accuracy.  It is worth mentioning that if you
 * can remove lens distortion then you also know the intrinsic camera parameters.  To see how this added information
 * can be used take a look at {@link boofcv.examples.stereo.ExampleStereoTwoViewsOneCamera}.
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
		// Unrectified images
		MultiSpectral<ImageFloat32> unrectLeft =
				ConvertBufferedImage.convertFromMulti(origLeft, null,true, ImageFloat32.class);
		MultiSpectral<ImageFloat32> unrectRight =
				ConvertBufferedImage.convertFromMulti(origRight, null,true, ImageFloat32.class);

		// storage for rectified images
		MultiSpectral<ImageFloat32> rectLeft = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				unrectLeft.getWidth(),unrectLeft.getHeight(),unrectLeft.getNumBands());
		MultiSpectral<ImageFloat32> rectRight = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				unrectRight.getWidth(),unrectRight.getHeight(),unrectRight.getNumBands());

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
		ImageDistort<ImageFloat32,ImageFloat32> imageDistortLeft =
				RectifyImageOps.rectifyImage(rect1,ImageFloat32.class);
		ImageDistort<ImageFloat32,ImageFloat32> imageDistortRight =
				RectifyImageOps.rectifyImage(rect2,ImageFloat32.class);

		DistortImageOps.distortMS(unrectLeft, rectLeft, imageDistortLeft);
		DistortImageOps.distortMS(unrectRight, rectRight, imageDistortRight);

		// convert for output
		BufferedImage outLeft = ConvertBufferedImage.convertTo(rectLeft,null,true);
		BufferedImage outRight = ConvertBufferedImage.convertTo(rectRight, null,true);

		// show results and draw a horizontal line where the user clicks to see rectification easier
		// Don't worry if the image appears upside down
		ShowImages.showWindow(new RectifiedPairPanel(true, outLeft,outRight),"Rectified");
	}

	public static void main( String args[] ) {

		// Load images with lens distortion removed.  If lens distortion has not been
		// removed then the results will be approximate
		String dir = "../data/applet/stereo/";
		BufferedImage imageA = UtilImageIO.loadImage(dir + "mono_wall_01_undist.jpg");
		BufferedImage imageB = UtilImageIO.loadImage(dir + "mono_wall_03_undist.jpg");

		// Find a set of point feature matches
		List<AssociatedPair> matches = ExampleFundamentalMatrix.computeMatches(imageA, imageB);

		// Prune matches using the epipolar constraint
		List<AssociatedPair> inliers = new ArrayList<AssociatedPair>();
		DenseMatrix64F F = ExampleFundamentalMatrix.robustFundamental(matches, inliers);

		// display the inlier matches found using the robust estimator
		AssociationPanel panel = new AssociationPanel(20);
		panel.setAssociation(inliers);
		panel.setImages(imageA,imageB);

		ShowImages.showWindow(panel, "Inlier Pairs");

		rectify(F,inliers,imageA,imageB);
	}
}
