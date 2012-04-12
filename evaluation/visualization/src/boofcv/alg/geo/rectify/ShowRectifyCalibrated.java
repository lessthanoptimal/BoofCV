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

package boofcv.alg.geo.rectify;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.UtilEpipolar;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class ShowRectifyCalibrated {

	public static void main( String args[] ) {
		StereoParameters param = BoofMiscOps.loadXML("stereo.xml");

		String dir = "../data/evaluation/calibration/stereo/Bumblebee2_Chess/";

		// load images
		BufferedImage origLeft = UtilImageIO.loadImage(dir+"left05.jpg");
		BufferedImage origRight = UtilImageIO.loadImage(dir+"right05.jpg");

		// distorted images
		MultiSpectral<ImageFloat32> distLeft = ConvertBufferedImage.convertFromMulti(origLeft, null, ImageFloat32.class);
		MultiSpectral<ImageFloat32> distRight = ConvertBufferedImage.convertFromMulti(origRight, null, ImageFloat32.class);

		// storage for rectified images
		MultiSpectral<ImageFloat32> rectLeft = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				distLeft.getWidth(),distLeft.getHeight(),distLeft.getNumBands());
		MultiSpectral<ImageFloat32> rectRight = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				distRight.getWidth(),distRight.getHeight(),distRight.getNumBands());

		// Compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = param.getRightToLeft().invert(null);
		DenseMatrix64F K1 = UtilEpipolar.calibrationMatrix(param.getLeft());
		DenseMatrix64F K2 = UtilEpipolar.calibrationMatrix(param.getRight());

		rectifyAlg.process(K1,new Se3_F64(),K2,leftToRight);

		// TODO adjust rectification to maximize view area

		// undistorted images
		ImageDistort<ImageFloat32> imageDistortLeft =
				RectifyImageOps.rectifyImage(param.getLeft(), true, rectifyAlg.getRect1(), ImageFloat32.class);
		ImageDistort<ImageFloat32> imageDistortRight =
				RectifyImageOps.rectifyImage(param.getRight(),true, rectifyAlg.getRect2(),ImageFloat32.class);

		DistortImageOps.distortMS(distLeft, rectLeft, imageDistortLeft);
		DistortImageOps.distortMS(distRight, rectRight, imageDistortRight);

		// convert for output
		BufferedImage outLeft = ConvertBufferedImage.convertTo(rectLeft,null);
		BufferedImage outRight = ConvertBufferedImage.convertTo(rectRight, null);

		ShowImages.showWindow(origLeft,"Original Left");
		ShowImages.showWindow(origRight,"Original Right");
		ShowImages.showWindow(outLeft,"Rectified Left");
		ShowImages.showWindow(outRight,"Rectified Right");
	}
}
