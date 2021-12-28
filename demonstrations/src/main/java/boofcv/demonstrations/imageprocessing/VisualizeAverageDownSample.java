/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.imageprocessing;

import boofcv.abst.distort.FDistort;
import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Visualizes average down sampling.
 *
 * @author Peter Abeles
 */
public class VisualizeAverageDownSample {
	public static void main( String[] args ) {
		BufferedImage original = UtilImageIO.loadImage(UtilIO.pathExample("simple_objects.jpg"));
		Objects.requireNonNull(original);

		Planar<GrayF32> input = new Planar<>(GrayF32.class,
				original.getWidth(), original.getHeight(), 3);

		ConvertBufferedImage.convertFromPlanar(original, input, true, GrayF32.class);

		Planar<GrayF32> output = new Planar<>(GrayF32.class,
				original.getWidth()/3, original.getHeight()/3, 3);
		Planar<GrayF32> output2 = new Planar<>(GrayF32.class,
				original.getWidth()/3, original.getHeight()/3, 3);

		AverageDownSampleOps.down(input, output);
		new FDistort(input, output2).scaleExt().apply();

		BufferedImage outputFull = ConvertBufferedImage.convertTo_F32(output, null, true);
		BufferedImage outputFull2 = ConvertBufferedImage.convertTo_F32(output2, null, true);

		ShowImages.showWindow(original, "Original");
		ShowImages.showWindow(outputFull, "3x small average");
		ShowImages.showWindow(outputFull2, "3x small bilinear");
	}
}
