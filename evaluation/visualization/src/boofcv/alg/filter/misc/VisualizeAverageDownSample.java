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

package boofcv.alg.filter.misc;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class VisualizeAverageDownSample {
	public static void main(String[] args) {
		BufferedImage original = UtilImageIO.loadImage("../data/applet/simple_objects.jpg");

		MultiSpectral<ImageFloat32> input = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				original.getWidth(),original.getHeight(),3);

		ConvertBufferedImage.convertFromMulti(original,input,true,ImageFloat32.class);

		MultiSpectral<ImageFloat32> output = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				original.getWidth()/3,original.getHeight()/3,3);
		MultiSpectral<ImageFloat32> output2 = new MultiSpectral<ImageFloat32>(ImageFloat32.class,
				original.getWidth()/3,original.getHeight()/3,3);

		AverageDownSampleOps.down(input,output);
		DistortImageOps.scale(input,output2, TypeInterpolate.BILINEAR);

		BufferedImage outputFull = ConvertBufferedImage.convertTo_F32(output, null, true);
		BufferedImage outputFull2 = ConvertBufferedImage.convertTo_F32(output2, null, true);

		ShowImages.showWindow(original,"Original");
		ShowImages.showWindow(outputFull,"3x small average");
		ShowImages.showWindow(outputFull2,"3x small bilinear");
	}
}
