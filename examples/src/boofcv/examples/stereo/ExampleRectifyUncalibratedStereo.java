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

package boofcv.examples.stereo;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyFundamental;
import boofcv.core.image.border.BorderType;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

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
// TODO this is upside down to remind me to make this algorithm more stable
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
	public static void rectify( DMatrixRMaj F , List<AssociatedPair> inliers ,
								BufferedImage origLeft , BufferedImage origRight ) {
		// Unrectified images
		Planar<GrayF32> unrectLeft =
				ConvertBufferedImage.convertFromPlanar(origLeft, null,true, GrayF32.class);
		Planar<GrayF32> unrectRight =
				ConvertBufferedImage.convertFromPlanar(origRight, null,true, GrayF32.class);

		// storage for rectified images
		Planar<GrayF32> rectLeft = unrectLeft.createSameShape();
		Planar<GrayF32> rectRight = unrectRight.createSameShape();

		// Compute rectification
		RectifyFundamental rectifyAlg = RectifyImageOps.createUncalibrated();

		rectifyAlg.process(F,inliers,origLeft.getWidth(),origLeft.getHeight());

		// rectification matrix for each image
		DMatrixRMaj rect1 = rectifyAlg.getRect1();
		DMatrixRMaj rect2 = rectifyAlg.getRect2();

		// Adjust the rectification to make the view area more useful
		RectifyImageOps.fullViewLeft(origLeft.getWidth(),origLeft.getHeight(), rect1, rect2 );
//		RectifyImageOps.allInsideLeft(origLeft.getWidth(),origLeft.getHeight(), rect1, rect2 );

		// undistorted and rectify images
		FMatrixRMaj rect1_F32 = new FMatrixRMaj(3,3); // TODO simplify code some how
		FMatrixRMaj rect2_F32 = new FMatrixRMaj(3,3);
		ConvertMatrixData.convert(rect1, rect1_F32);
		ConvertMatrixData.convert(rect2, rect2_F32);

		ImageDistort<GrayF32,GrayF32> imageDistortLeft =
				RectifyImageOps.rectifyImage(rect1_F32, BorderType.SKIP, GrayF32.class);
		ImageDistort<GrayF32,GrayF32> imageDistortRight =
				RectifyImageOps.rectifyImage(rect2_F32, BorderType.SKIP, GrayF32.class);

		DistortImageOps.distortPL(unrectLeft, rectLeft, imageDistortLeft);
		DistortImageOps.distortPL(unrectRight, rectRight, imageDistortRight);

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
		String dir = UtilIO.pathExample("stereo/");
		BufferedImage imageA = UtilImageIO.loadImage(dir , "mono_wall_01_undist.jpg");
		BufferedImage imageB = UtilImageIO.loadImage(dir , "mono_wall_03_undist.jpg");

		// Find a set of point feature matches
		List<AssociatedPair> matches = ExampleFundamentalMatrix.computeMatches(imageA, imageB);

		// Prune matches using the epipolar constraint
		List<AssociatedPair> inliers = new ArrayList<>();
		DMatrixRMaj F = ExampleFundamentalMatrix.robustFundamental(matches, inliers);

		// display the inlier matches found using the robust estimator
		AssociationPanel panel = new AssociationPanel(20);
		panel.setAssociation(inliers);
		panel.setImages(imageA,imageB);

		ShowImages.showWindow(panel, "Inlier Pairs");

		rectify(F,inliers,imageA,imageB);
	}
}
