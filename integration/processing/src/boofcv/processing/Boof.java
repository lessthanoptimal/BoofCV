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

package boofcv.processing;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

/**
 * Convince class for Processing.
 *
 * @author Peter Abeles
 */
// TODO Binary image ops example
// TODO ellipse fitting example for particles

// TODO KLT tracker
// TODO Object tracker
// TODO Dense flow
// TODO Detect corners
// TODO Detect SURF
// TODO Associate two images
// TODO Compute homography
// TODO Apply homography

public class Boof {

	PApplet parent;

	public Boof(PApplet parent) {
		this.parent = parent;
	}

	public static SimpleGray convF32(PImage image) {
		ImageFloat32 out = new ImageFloat32(image.width,image.height);

		switch( image.format ) {
			case PConstants.RGB :
			case PConstants.ARGB:
				ConvertProcessing.convert_RGB_F32(image,out);
				break;

			default:
				throw new RuntimeException("Unsupported image type");
		}

		return new SimpleGray(out);
	}

	public static SimpleGray convU8(PImage image) {
		ImageUInt8 out = new ImageUInt8(image.width,image.height);

		switch( image.format ) {
			case PConstants.RGB :
			case PConstants.ARGB:
				ConvertProcessing.convert_RGB_U8(image,out);
				break;

			default:
				throw new RuntimeException("Unsupported image type");
		}

		return new SimpleGray(out);
	}






}
