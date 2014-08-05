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

package boofcv.alg.ip;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class VisualizeFlipRotate {
	public static void main(String[] args) {
		BufferedImage input = UtilImageIO.loadImage("../data/evaluation/sunflowers.png");
		ImageUInt8 gray = ConvertBufferedImage.convertFrom(input,(ImageUInt8)null);

		ImageUInt8 flipH = gray.clone();
		ImageUInt8 flipV = gray.clone();
		ImageUInt8 rotateCW = new ImageUInt8(gray.height,gray.width);
		ImageUInt8 rotateCCW = new ImageUInt8(gray.height,gray.width);

		ImageMiscOps.flipHorizontal(flipH);
		ImageMiscOps.flipVertical(flipV);
		ImageMiscOps.rotateCW(gray, rotateCW);
		ImageMiscOps.rotateCCW(gray, rotateCCW);

		ShowImages.showWindow(gray,"Input");
		ShowImages.showWindow(flipH,"Flip Horizontal");
		ShowImages.showWindow(flipV,"Flip Vertical");
		ShowImages.showWindow(rotateCW,"Rotate CW");
		ShowImages.showWindow(rotateCCW,"Rotate CCW");

	}
}
