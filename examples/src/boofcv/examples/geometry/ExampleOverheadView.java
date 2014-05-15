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

package boofcv.examples.geometry;

import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.sfm.overhead.CreateSyntheticOverheadView;
import boofcv.alg.sfm.overhead.CreateSyntheticOverheadViewMS;
import boofcv.alg.sfm.overhead.SelectOverheadParameters;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.se.Se3_F64;

import java.awt.image.BufferedImage;

/**
 * Creates a synthetic overhead view from an image using the known ground plane.  This is commonly used for navigation
 * in man-made environments, such on roads or through hallways.  Objects which are not actually on the ground plane
 * will be heavily distorted in the overhead view.
 *
 * @author Peter Abeles
 */
public class ExampleOverheadView {
	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage("../data/applet/road/left01.png");

		MultiSpectral<ImageUInt8> imageRGB = ConvertBufferedImage.convertFromMulti(input, null,true, ImageUInt8.class);

		StereoParameters stereoParam = UtilIO.loadXML("../data/applet/road/stereo01.xml");
		Se3_F64 groundToLeft = UtilIO.loadXML("../data/applet/road/ground_to_left_01.xml");

		CreateSyntheticOverheadView<MultiSpectral<ImageUInt8>> generateOverhead =
				new CreateSyntheticOverheadViewMS<ImageUInt8>(TypeInterpolate.BILINEAR,3,ImageUInt8.class);

		// size of cells in the overhead image in world units
		double cellSize = 0.05;

		// You can use this to automatically select reasonable values for the overhead image
		SelectOverheadParameters selectMapSize = new SelectOverheadParameters(cellSize,20,0.5);
		selectMapSize.process(stereoParam.left,groundToLeft);

		int overheadWidth = selectMapSize.getOverheadWidth();
		int overheadHeight = selectMapSize.getOverheadHeight();

		MultiSpectral<ImageUInt8> overheadRGB =
				new MultiSpectral<ImageUInt8>(ImageUInt8.class,overheadWidth,overheadHeight,3);
		generateOverhead.configure(stereoParam.left,groundToLeft,
				selectMapSize.getCenterX(), selectMapSize.getCenterY(), cellSize,overheadRGB.width,overheadRGB.height);

		generateOverhead.process(imageRGB, overheadRGB);

		// note that the left/right values are swapped in the overhead image.  This is an artifact of the plane's
		// 2D coordinate system having +y pointing up, while images have +y pointing down.
		BufferedImage output = ConvertBufferedImage.convertTo(overheadRGB,null,true);

		ShowImages.showWindow(input,"Input Image");
		ShowImages.showWindow(output,"Overhead Image");
	}
}
