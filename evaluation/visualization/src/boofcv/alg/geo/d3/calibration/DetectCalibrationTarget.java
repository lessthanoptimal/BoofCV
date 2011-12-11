/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d3.calibration;

import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class DetectCalibrationTarget<T extends ImageBase> {
	Class<T> imageType;

	// number of black squares in the horizontal and vertical directions
	int gridWidth;
	int gridHeight;

	ImageUInt8 threshold = new ImageUInt8(1,1);


	public DetectCalibrationTarget(Class<T> imageType , int gridWidth , int gridHeight ) {
		this.imageType = imageType;
		this.gridWidth = gridWidth;
		this.gridHeight = gridHeight;
	}

	public void process( T image ) {
		threshold.reshape(image.width,image.height);

		GThresholdImageOps.threshold(image,threshold,30,true);

		// filter out small objects

		// find blobs

		// find blob contours

		// remove blobs which are not like a polygon at all

		// use original binary image to find corners

		// optimize corners

		BufferedImage b = VisualizeBinaryData.renderBinary(threshold, null);

		ShowImages.showWindow(b,"Threshold");
	}

	public static void main( String args[] ) {
		DetectCalibrationTarget<ImageUInt8> app = new DetectCalibrationTarget<ImageUInt8>(ImageUInt8.class,4,3);

		ImageUInt8 input = UtilImageIO.loadImage("data/calibration/Sony_DSC-HX5V/image01.jpg",ImageUInt8.class);
		app.process(input);

	}
}
