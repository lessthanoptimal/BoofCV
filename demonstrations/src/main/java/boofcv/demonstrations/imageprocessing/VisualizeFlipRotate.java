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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Visualizes flip and rotate operations
 *
 * @author Peter Abeles
 */
public class VisualizeFlipRotate {
	public static void main( String[] args ) {
		BufferedImage input = UtilImageIO.loadImage(UtilIO.pathExample("sunflowers.jpg"));
		Objects.requireNonNull(input);
		GrayU8 gray = ConvertBufferedImage.convertFrom(input, (GrayU8)null);

		GrayU8 flipH = gray.clone();
		GrayU8 flipV = gray.clone();

		ImageMiscOps.flipHorizontal(flipH);
		ImageMiscOps.flipVertical(flipV);
		GrayU8 rotateCW = ImageMiscOps.rotateCW(gray, null);
		GrayU8 rotateCCW = ImageMiscOps.rotateCCW(gray, null);
		GrayU8 transposed = ImageMiscOps.transpose(gray, null);

		var panel = new ListDisplayPanel();
		panel.addImage(gray, "Input");
		panel.addImage(flipH, "Flip Horizontal");
		panel.addImage(flipV, "Flip Vertical");
		panel.addImage(rotateCW, "Rotate CW");
		panel.addImage(rotateCCW, "Rotate CCW");
		panel.addImage(transposed, "Transposed");

		ShowImages.showWindow(panel, "Flip-Rotate-Transpose", true);
	}
}
