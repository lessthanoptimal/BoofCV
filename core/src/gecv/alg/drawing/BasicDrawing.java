/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.drawing;

import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;

/**
 * Basic operations for drawing shapes and other patterns into different image types.  
 *
 * @author Peter Abeles
 */
// todo add noise
public class BasicDrawing {

	/**
	 * Fills the whole image with the specified pixel value
	 *
	 * @param image The image which is to be filled image.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( ImageUInt8 image , int value ) {
		BasicDrawing_I8.fill(image,value);
	}

	public static void fill( ImageSInt16 image , int value ) {

	}

	public static void fill( ImageSInt32 image , int value ) {

	}

	public static void fill( ImageFloat32 image , float value ) {

	}

	/**
	 * Draws a rectangle aligned along the image's axis into the image.
	 *
	 * @param img The image the rectangle is to be drawn inside of
	 * @param value Value of pixels inside the rectangle.
	 * @param x0 Top left corner x-axis.
	 * @param y0 Top left corner y-axis.
	 * @param x1 Bottom right corner x-axis.
	 * @param y1 Bottom right corner y-axis.
	 */
	public static void rectangle( ImageUInt8 img , int value , int x0 , int y0 , int x1 , int y1 ) {
		BasicDrawing_I8.rectangle(img,value,x0,y0,x1,y1);
	}
}
