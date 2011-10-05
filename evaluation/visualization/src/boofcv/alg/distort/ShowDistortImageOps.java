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

package boofcv.alg.distort;

import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Shows the result of different blur operations
 *
 * @author Peter Abeles
 */
// todo convert
public class ShowDistortImageOps
{
	public static void main( String args[] ) {
		BufferedImage image = UtilImageIO.loadImage("data/sunflowers.png");
		ImageFloat32  input = ConvertBufferedImage.convertFrom(image,null,ImageFloat32.class);

		ImageFloat32 output = new ImageFloat32(input.height,input.width);

		DistortImageOps.rotate(input,output, TypeInterpolate.BILINEAR,(float)(Math.PI/2.0));

		BufferedImage showImg = ConvertBufferedImage.convertTo(output,null);

		ShowImages.showWindow(showImg,"Rotated");
	}
}
