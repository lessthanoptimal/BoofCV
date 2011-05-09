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

package gecv.alg.filter.basic;

import gecv.alg.InputSanityCheck;
import gecv.alg.filter.basic.impl.BinaryEdgeOps;
import gecv.alg.filter.basic.impl.BinaryInnerOps;
import gecv.alg.filter.basic.impl.BinaryNaiveOps;
import gecv.struct.image.ImageUInt8;

/**
 * <p>
 * Contains a standard set of operations performed on binary images. A pixel has a value of false if it is equal
 * to zero or true equal to one.
 * </p>
 * <p/>
 * <p>
 * NOTE: If an element's value is not zero or one then each function's behavior is undefined.
 * </p>
 *
 * @author Peter Abeles
 */
/*
 * DESIGN NOTE: 8-bit integer images ({@link ImageUInt8}) are used instead of images composed of boolean values because
 * there is no performance advantage.  According to the virtual machines specification binary arrays are stored as
 * byte arrays with 1 representing true and 0 representing false.

 * DESIGN NOTE: Restricting input values to zero and one was tested was compared against defining true as not zero.
 * The former allowed a 2x to 3x performance boost by allowing numbers to be summed instead of compared.
 */
public class BinaryImageOps {

	/**
	 * <p>
	 * Erodes an image according to a 4-neighborhood.  Unless a pixel is connected to all its neighbors its value
	 * is set to zero.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageUInt8 erode4(ImageUInt8 input, ImageUInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		BinaryInnerOps.erode4(input, output);
		BinaryEdgeOps.erode4(input, output);

		return output;
	}

	/**
	 * <p>
	 * Dilates an image according to a 4-neighborhood.  If a pixel is connected to any other pixel then its output
	 * value will be one.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageUInt8 dilate4(ImageUInt8 input, ImageUInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		BinaryInnerOps.dilate4(input, output);
		BinaryEdgeOps.dilate4(input, output);

		return output;
	}

	/**
	 * <p>
	 * Binary operation which is designed to remove all pixels but ones which are on the edge of an object.
	 * The edge is defined as lying on the object and not being surrounded by a pixel along a 4-neighborhood.
	 * </p>
	 * <p/>
	 * <p>
	 * NOTE: There are many ways to define an edge, this is just one of them.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageUInt8 edge4(ImageUInt8 input, ImageUInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		BinaryNaiveOps.edge4(input, output);
		BinaryInnerOps.edge4(input, output);
		BinaryEdgeOps.edge4(input, output);

		return output;
	}

	/**
	 * <p>
	 * Erodes an image according to a 8-neighborhood.  Unless a pixel is connected to all its neighbors its value
	 * is set to zero.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageUInt8 erode8(ImageUInt8 input, ImageUInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		BinaryInnerOps.erode8(input, output);
		BinaryEdgeOps.erode8(input, output);

		return output;
	}

	/**
	 * <p>
	 * Dilates an image according to a 8-neighborhood.  If a pixel is connected to any other pixel then its output
	 * value will be one.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageUInt8 dilate8(ImageUInt8 input, ImageUInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		BinaryInnerOps.dilate8(input, output);
		BinaryEdgeOps.dilate8(input, output);

		return output;
	}

	/**
	 * <p>
	 * Binary operation which is designed to remove all pixels but ones which are on the edge of an object.
	 * The edge is defined as lying on the object and not being surrounded by 8 pixels.
	 * </p>
	 * <p/>
	 * <p>
	 * NOTE: There are many ways to define an edge, this is just one of them.
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageUInt8 edge8(ImageUInt8 input, ImageUInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		BinaryInnerOps.edge8(input, output);
		BinaryEdgeOps.edge8(input, output);

		return output;
	}

	/**
	 * Binary operation which is designed to remove small bits of spurious noise.  An 8-neighborhood is used.
	 * If a pixel is connected to less than 2 neighbors then its value zero.  If connected to more than 6 then
	 * its value is one.  Otherwise it retains its original value.
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static ImageUInt8 removePointNoise(ImageUInt8 input, ImageUInt8 output) {
		output = InputSanityCheck.checkDeclare(input, output);

		BinaryInnerOps.removePointNoise(input, output);
		BinaryEdgeOps.removePointNoise(input, output);

		return output;
	}
}
