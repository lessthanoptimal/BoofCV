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

package boofcv.app.calib;

import boofcv.alg.distort.RemovePerspectiveDistortion;
import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.border.BorderType;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Selects which image to same based on how sharp it is and then saves them to the disk
 *
 * @author Peter Abeles
 */
public class ImageSelectorAndSaver {

	public static int LENGTH = 50;

	RemovePerspectiveDistortion<GrayF32> removePerspective =
			new RemovePerspectiveDistortion<>(LENGTH, LENGTH, ImageType.single(GrayF32.class));

	GrayF32 templateOriginal = new GrayF32(LENGTH, LENGTH);
	GrayF32 template = new GrayF32(LENGTH, LENGTH);
	GrayF32 weights = new GrayF32(LENGTH, LENGTH);
	double totalWeight;


	GrayF32 difference = new GrayF32(LENGTH, LENGTH);
	GrayF32 tempImage = new GrayF32(LENGTH, LENGTH);

	BufferedImage bestImage;
	double bestScore;

	double currentScore;

	File outputDirectory;
	int imageNumber = 0;

	public ImageSelectorAndSaver(String outputDirectory) {
		this.outputDirectory = new File(outputDirectory);

		if( !this.outputDirectory.exists() ) {
			if( !this.outputDirectory.mkdirs() ) {
				throw new RuntimeException("Can't create output directory. "+this.outputDirectory.getPath());
			}
		}
		// TODO see if there are images in the output directory.  Ask if it should delete it
	}

	/**
	 * Creates a template of the fiducial and this is then used to determine how blurred the image is
	 */
	public void setTemplate(GrayF32 image, List<Point2D_F64> sides) {
		if( sides.size() != 4 )
			throw new IllegalArgumentException("Expected 4 sidesCollision");

		removePerspective.apply(image,sides.get(0),sides.get(1),sides.get(2),sides.get(3));

		templateOriginal.setTo(removePerspective.getOutput());

		// blur the image a bit so it doesn't have to be a perfect match
		GrayF32 blurred = new GrayF32(LENGTH,LENGTH);
		BlurImageOps.gaussian(templateOriginal,blurred,-1,2,null);

		// place greater importance on pixels which are around edges
		GrayF32 derivX = new GrayF32(LENGTH, LENGTH);
		GrayF32 derivY = new GrayF32(LENGTH, LENGTH);
		GImageDerivativeOps.gradient(DerivativeType.SOBEL,blurred,derivX,derivY, BorderType.EXTENDED);

		GGradientToEdgeFeatures.intensityE(derivX,derivY,weights);

		float max = ImageStatistics.max(weights);
		PixelMath.divide(weights,max,weights);

		totalWeight = ImageStatistics.sum(weights);

		// compute a normalized template for later use.  Divide by the mean to add some lighting invariance
		template.setTo(removePerspective.getOutput());
		float mean = (float)ImageStatistics.mean(template);
		PixelMath.divide(template,mean,template);
	}

	/**
	 * Discard the current best image
	 */
	public synchronized void clearHistory() {
		bestScore = Double.MAX_VALUE;
		bestImage = null;
	}

	/**
	 * Computes the sharpness score for the current image, if better than the current best image it's then saved.
	 * @param image Gray scale input image for detector
	 * @param sides Location of 4 corners on fiducial
	 */
	public synchronized void process(GrayF32 image, List<Point2D_F64> sides) {
		if( sides.size() != 4 )
			throw new IllegalArgumentException("Expected 4 sidesCollision");

		updateScore(image,sides);

		if( currentScore < bestScore ) {
			bestScore = currentScore;
			if( bestImage == null ) {
				bestImage = new BufferedImage(image.getWidth(), image.getHeight(),BufferedImage.TYPE_INT_RGB);
			}
			ConvertBufferedImage.convertTo(image,bestImage);
		}
	}

	/**
	 * Used when you just want to update the score for visualization purposes but not update the best image.
	 */
	public synchronized void updateScore(GrayF32 image, List<Point2D_F64> sides) {
		removePerspective.apply(image,sides.get(0),sides.get(1),sides.get(2),sides.get(3));

		GrayF32 current = removePerspective.getOutput();
		float mean = (float)ImageStatistics.mean(current);
		PixelMath.divide(current,mean,tempImage);

		PixelMath.diffAbs(tempImage,template,difference);
		PixelMath.multiply(difference,weights,difference);

		// compute score as a weighted average of the difference
		currentScore = ImageStatistics.sum(difference)/totalWeight;
	}

	/**
	 * Saves the image to a file
	 */
	public synchronized void save() {
		if( bestImage != null ) {
			File path = new File(outputDirectory, String.format("image%04d.png",imageNumber));
			UtilImageIO.saveImage(bestImage,path.getAbsolutePath());
			imageNumber++;
		}
		clearHistory();
	}

	public double getFocusScore() {
		return currentScore;
	}

	public GrayF32 getTemplate() {
		return templateOriginal;
	}

	public GrayF32 getCurrentView() {
		return removePerspective.getOutput();
	}
}
