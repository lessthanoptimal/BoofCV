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

import boofcv.alg.distort.RemovePerspectiveDistortion;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F64;

import java.awt.image.BufferedImage;

/**
 * Certain image processing techniques, such as Optical Character Recognition (OCR), can be performed better if
 * perspective distortion is remove from an image.  In this example a homography is computed from the four corners
 * of a bulletin board and the image is projected into a square image without perspective distortion.  The
 * {@link RemovePerspectiveDistortion} class is used to perform the distortion.  The class is easy to understand
 * if you know what a homography is, you should look at it!
 *
 * @author Peter Abeles
 */
public class ExampleRemovePerspectiveDistortion {
	public static void main(String[] args) {

		// load a color image
		BufferedImage buffered = UtilImageIO.loadImage(UtilIO.pathExample("goals_and_stuff.jpg"));
		Planar<GrayF32> input = ConvertBufferedImage.convertFromPlanar(buffered, null, true, GrayF32.class);

		RemovePerspectiveDistortion<Planar<GrayF32>> removePerspective =
				new RemovePerspectiveDistortion<>(400, 500, ImageType.pl(3, GrayF32.class));

		// Specify the corners in the input image of the region.
		// Order matters! top-left, top-right, bottom-right, bottom-left
		if( !removePerspective.apply(input,
				new Point2D_F64(267, 182), new Point2D_F64(542, 68),
				new Point2D_F64(519, 736), new Point2D_F64(276, 570)) ){
			throw new RuntimeException("Failed!?!?");
		}

		Planar<GrayF32> output = removePerspective.getOutput();

		BufferedImage flat = ConvertBufferedImage.convertTo_F32(output,null,true);
		ShowImages.showWindow(buffered,"Original Image",true);
		ShowImages.showWindow(flat,"Without Perspective Distortion",true);
	}
}
