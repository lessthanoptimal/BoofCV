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

package boofcv.alg.filter.binary;

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.abst.filter.binary.BinaryContourInterface;
import boofcv.abst.filter.binary.BinaryLabelContourFinder;
import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.binary.impl.*;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.filter.binary.FactoryBinaryContourFinder;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <p>
 * Contains a standard set of operations performed on binary images. A pixel has a value of false if it is equal
 * to zero or true equal to one.
 * </p>
 *
 * <p>
 * NOTE: If an element's value is not zero or one then each function's behavior is undefined.
 * </p>
 *
 * @author Peter Abeles
 */
/*
 * DESIGN NOTE: 8-bit integer images ({@link GrayU8}) are used instead of images composed of boolean values because
 * there is no performance advantage. According to the virtual machines specification binary arrays are stored as
 * byte arrays with 1 representing true and 0 representing false.

 * DESIGN NOTE: Restricting input values to zero and one was tested was compared against defining true as not zero.
 * The former allowed a 2x to 3x performance boost by allowing numbers to be summed instead of compared.
 */
public class BinaryImageOps {

	/**
	 * For each pixel it applies the logical 'and' operator between two images.
	 *
	 * @param inputA First input image. Not modified.
	 * @param inputB Second input image. Not modified.
	 * @param output Output image. Can be same as either input. If null a new instance will be declared, Modified.
	 * @return Output of logical operation.
	 */
	public static GrayU8 logicAnd( GrayU8 inputA, GrayU8 inputB, @Nullable GrayU8 output ) {
		InputSanityCheck.checkSameShape(inputA, inputB);
		output = InputSanityCheck.declareOrReshape(inputA, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryImageOps_MT.logicAnd(inputA, inputB, output);
		} else {
			ImplBinaryImageOps.logicAnd(inputA, inputB, output);
		}

		return output;
	}

	/**
	 * For each pixel it applies the logical 'or' operator between two images.
	 *
	 * @param inputA First input image. Not modified.
	 * @param inputB Second input image. Not modified.
	 * @param output Output image. Can be same as either input. If null a new instance will be declared, Modified.
	 * @return Output of logical operation.
	 */
	public static GrayU8 logicOr( GrayU8 inputA, GrayU8 inputB, @Nullable GrayU8 output ) {
		InputSanityCheck.checkSameShape(inputA, inputB);
		output = InputSanityCheck.declareOrReshape(inputA, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryImageOps_MT.logicOr(inputA, inputB, output);
		} else {
			ImplBinaryImageOps.logicOr(inputA, inputB, output);
		}

		return output;
	}

	/**
	 * For each pixel it applies the logical 'xor' operator between two images.
	 *
	 * @param inputA First input image. Not modified.
	 * @param inputB Second input image. Not modified.
	 * @param output Output image. Can be same as either input. If null a new instance will be declared, Modified.
	 * @return Output of logical operation.
	 */
	public static GrayU8 logicXor( GrayU8 inputA, GrayU8 inputB, @Nullable GrayU8 output ) {
		InputSanityCheck.checkSameShape(inputA, inputB);
		output = InputSanityCheck.declareOrReshape(inputA, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryImageOps_MT.logicXor(inputA, inputB, output);
		} else {
			ImplBinaryImageOps.logicXor(inputA, inputB, output);
		}

		return output;
	}

	/**
	 * Inverts each pixel from true to false and vis-versa.
	 *
	 * @param input Input image. Not modified.
	 * @param output Output image. Can be same as input. If null a new instance will be declared, Modified.
	 * @return Output of logical operation.
	 */
	public static GrayU8 invert( GrayU8 input, @Nullable GrayU8 output ) {
		output = InputSanityCheck.declareOrReshape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryImageOps_MT.invert(input, output);
		} else {
			ImplBinaryImageOps.invert(input, output);
		}

		return output;
	}

	/**
	 * <p>
	 * Erodes an image according to a 4-neighborhood. Unless a pixel is connected to all its neighbors its value
	 * is set to zero.
	 * </p>
	 *
	 * @param input Input image. Not modified.
	 * @param numTimes How many times the operation will be applied to the image.
	 * @param output If not null, the output image. If null a new image is declared and returned. Modified.
	 * @return Output image.
	 */
	public static GrayU8 erode4( GrayU8 input, int numTimes, @Nullable GrayU8 output ) {
		output = InputSanityCheck.declareOrReshape(input, output);

		if (numTimes <= 0)
			throw new IllegalArgumentException("numTimes must be >= 1");

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryInnerOps_MT.erode4(input, output);
		} else {
			ImplBinaryInnerOps.erode4(input, output);
		}
		ImplBinaryBorderOps.erode4(input, output);


		if (numTimes > 1) {
			GrayU8 tmp1 = new GrayU8(input.width, input.height);
			GrayU8 tmp2 = output;

			for (int i = 1; i < numTimes; i++) {
				if (BoofConcurrency.USE_CONCURRENT) {
					ImplBinaryInnerOps_MT.erode4(tmp2, tmp1);
				} else {
					ImplBinaryInnerOps.erode4(tmp2, tmp1);
				}
				ImplBinaryBorderOps.erode4(tmp2, tmp1);

				GrayU8 a = tmp1;
				tmp1 = tmp2;
				tmp2 = a;
			}

			if (tmp2 != output) {
				output.setTo(tmp2);
			}
		}

		return output;
	}

	/**
	 * <p>
	 * Dilates an image according to a 4-neighborhood. If a pixel is connected to any other pixel then its output
	 * value will be one.
	 * </p>
	 *
	 * @param input Input image. Not modified.
	 * @param numTimes How many times the operation will be applied to the image.
	 * @param output If not null, the output image. If null a new image is declared and returned. Modified.
	 * @return Output image.
	 */
	public static GrayU8 dilate4( GrayU8 input, int numTimes, @Nullable GrayU8 output ) {
		output = InputSanityCheck.declareOrReshape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryInnerOps_MT.dilate4(input, output);
		} else {
			ImplBinaryInnerOps.dilate4(input, output);
		}
		ImplBinaryBorderOps.dilate4(input, output);

		if (numTimes > 1) {
			GrayU8 tmp1 = new GrayU8(input.width, input.height);
			GrayU8 tmp2 = output;

			for (int i = 1; i < numTimes; i++) {
				if (BoofConcurrency.USE_CONCURRENT) {
					ImplBinaryInnerOps_MT.dilate4(tmp2, tmp1);
				} else {
					ImplBinaryInnerOps.dilate4(tmp2, tmp1);
				}
				ImplBinaryBorderOps.dilate4(tmp2, tmp1);

				GrayU8 a = tmp1;
				tmp1 = tmp2;
				tmp2 = a;
			}

			if (tmp2 != output) {
				output.setTo(tmp2);
			}
		}

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
	 * @param input Input image. Not modified.
	 * @param output If not null, the output image. If null a new image is declared and returned. Modified.
	 * @param outsideZero if true then pixels outside the image are treated as zero, otherwise one
	 * @return Output image.
	 */
	public static GrayU8 edge4( GrayU8 input, GrayU8 output, boolean outsideZero ) {
		output = InputSanityCheck.declareOrReshape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryInnerOps_MT.edge4(input, output);
		} else {
			ImplBinaryInnerOps.edge4(input, output);
		}
		ImplBinaryBorderOps.edge4(input, output, outsideZero);

		return output;
	}

	/**
	 * <p>
	 * Erodes an image according to a 8-neighborhood. Unless a pixel is connected to all its neighbors its value
	 * is set to zero.
	 * </p>
	 *
	 * @param input Input image. Not modified.
	 * @param numTimes How many times the operation will be applied to the image.
	 * @param output If not null, the output image. If null a new image is declared and returned. Modified.
	 * @return Output image.
	 */
	public static GrayU8 erode8( GrayU8 input, int numTimes, @Nullable GrayU8 output ) {
		output = InputSanityCheck.declareOrReshape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryInnerOps_MT.erode8(input, output);
		} else {
			ImplBinaryInnerOps.erode8(input, output);
		}
		ImplBinaryBorderOps.erode8(input, output);

		if (numTimes > 1) {
			GrayU8 tmp1 = new GrayU8(input.width, input.height);
			GrayU8 tmp2 = output;

			for (int i = 1; i < numTimes; i++) {
				if (BoofConcurrency.USE_CONCURRENT) {
					ImplBinaryInnerOps_MT.erode8(tmp2, tmp1);
				} else {
					ImplBinaryInnerOps.erode8(tmp2, tmp1);
				}
				ImplBinaryBorderOps.erode8(tmp2, tmp1);

				GrayU8 a = tmp1;
				tmp1 = tmp2;
				tmp2 = a;
			}

			if (tmp2 != output) {
				output.setTo(tmp2);
			}
		}

		return output;
	}

	/**
	 * <p>
	 * Dilates an image according to a 8-neighborhood. If a pixel is connected to any other pixel then its output
	 * value will be one.
	 * </p>
	 *
	 * @param input Input image. Not modified.
	 * @param numTimes How many times the operation will be applied to the image.
	 * @param output If not null, the output image. If null a new image is declared and returned. Modified.
	 * @return Output image.
	 */
	public static GrayU8 dilate8( GrayU8 input, int numTimes, @Nullable GrayU8 output ) {
		output = InputSanityCheck.declareOrReshape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryInnerOps_MT.dilate8(input, output);
		} else {
			ImplBinaryInnerOps.dilate8(input, output);
		}
		ImplBinaryBorderOps.dilate8(input, output);

		if (numTimes > 1) {
			GrayU8 tmp1 = new GrayU8(input.width, input.height);
			GrayU8 tmp2 = output;

			for (int i = 1; i < numTimes; i++) {
				if (BoofConcurrency.USE_CONCURRENT) {
					ImplBinaryInnerOps_MT.dilate8(tmp2, tmp1);
				} else {
					ImplBinaryInnerOps.dilate8(tmp2, tmp1);
				}
				ImplBinaryBorderOps.dilate8(tmp2, tmp1);

				GrayU8 a = tmp1;
				tmp1 = tmp2;
				tmp2 = a;
			}

			if (tmp2 != output) {
				output.setTo(tmp2);
			}
		}

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
	 * @param input Input image. Not modified.
	 * @param output If not null, the output image. If null a new image is declared and returned. Modified.
	 * @param outsideZero if true then pixels outside the image are treated as zero, otherwise one
	 * @return Output image.
	 */
	public static GrayU8 edge8( GrayU8 input, GrayU8 output, boolean outsideZero ) {
		output = InputSanityCheck.declareOrReshape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryInnerOps_MT.edge8(input, output);
		} else {
			ImplBinaryInnerOps.edge8(input, output);
		}
		ImplBinaryBorderOps.edge8(input, output, outsideZero);

		return output;
	}

	/**
	 * Binary operation which is designed to remove small bits of spurious noise. An 8-neighborhood is used.
	 * If a pixel is connected to less than 2 neighbors then its value zero. If connected to more than 6 then
	 * its value is one. Otherwise it retains its original value.
	 *
	 * @param input Input image. Not modified.
	 * @param output If not null, the output image. If null a new image is declared and returned. Modified.
	 * @return Output image.
	 */
	public static GrayU8 removePointNoise( GrayU8 input, @Nullable GrayU8 output ) {
		output = InputSanityCheck.declareOrReshape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryInnerOps_MT.removePointNoise(input, output);
		} else {
			ImplBinaryInnerOps.removePointNoise(input, output);
		}
		ImplBinaryBorderOps.removePointNoise(input, output);

		return output;
	}

	/**
	 * Applies a morphological thinning operation to the image. Also known as skeletonization.
	 *
	 * @param input Input image. Not modified.
	 * @param maxIterations Maximum number of cycles it will thin for. -1 for the maximum required
	 * @param output If not null, the output image. If null a new image is declared and returned. Modified.
	 * @return Output image.
	 * @see BinaryThinning
	 */
	public static GrayU8 thin( GrayU8 input, int maxIterations, @Nullable GrayU8 output ) {

		output = InputSanityCheck.declareOrReshape(input, output);

		output.setTo(input);
		BinaryThinning thinning = new BinaryThinning();

		thinning.apply(output, maxIterations);

		return output;
	}

	/**
	 * <p>
	 * Given a binary image, connect together pixels to form blobs/clusters using the specified connectivity rule.
	 * The found blobs will be labeled in an output image and also described as a set of contours. Pixels
	 * in the contours are consecutive order in a clockwise or counter-clockwise direction, depending on the
	 * implementation. The labeled image will assign background pixels a label of 0 and each blob will be
	 * assigned a unique ID starting from 1.
	 * </p>
	 *
	 * <p>
	 * The returned contours are traces of the object. The trace of an object can be found by marking a point
	 * with a pen and then marking every point on the contour without removing the pen. It is possible to have
	 * the same point multiple times in the contour.
	 * </p>
	 *
	 * @param input Input binary image. Not modified.
	 * @param rule Connectivity rule. Can be 4 or 8. 8 is more commonly used.
	 * @param output (Optional) Output labeled image. If null, an image will be declared internally. Modified.
	 * @return List of found contours for each blob.
	 * @see LinearContourLabelChang2004
	 */
	public static List<Contour> contour( GrayU8 input, ConnectRule rule, @Nullable GrayS32 output ) {
		if (output == null) {
			output = new GrayS32(input.width, input.height);
		} else {
			InputSanityCheck.checkSameShape(input, output);
		}

		BinaryLabelContourFinder alg = FactoryBinaryContourFinder.linearChang2004();
		alg.setConnectRule(rule);
		alg.process(input, output);

		return convertContours(alg);
	}

	/**
	 * Finds the external contours only in the image
	 *
	 * @param input Input binary image. Not modified.
	 * @param rule Connectivity rule. Can be 4 or 8. 8 is more commonly used.
	 * @return List of found contours for each blob.
	 */
	public static List<Contour> contourExternal( GrayU8 input, ConnectRule rule ) {
		BinaryContourFinder alg = FactoryBinaryContourFinder.linearExternal();
		alg.setConnectRule(rule);
		alg.process(input);

		return convertContours(alg);
	}

	public static List<Contour> convertContours( BinaryContourInterface alg ) {

		List<ContourPacked> contours = alg.getContours();

		List<Contour> ret = new ArrayList<>();
		for (int i = 0; i < contours.size(); i++) {
			ContourPacked p = contours.get(i);
			Contour c = new Contour();
			c.external = BinaryContourInterface.copyContour(alg, p.externalIndex);

			for (int j = 0; j < p.internalIndexes.size; j++) {
				c.internal.add(BinaryContourInterface.copyContour(alg, p.internalIndexes.get(j)));
			}
			ret.add(c);
		}
		return ret;
	}

	/**
	 * Used to change the labels in a labeled binary image.
	 *
	 * @param input Labeled binary image.
	 * @param labels Look up table where the indexes are the current label and the value are its new value.
	 */
	public static void relabel( GrayS32 input, int[] labels ) {
		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryImageOps_MT.relabel(input, labels);
		} else {
			ImplBinaryImageOps.relabel(input, labels);
		}
	}

	/**
	 * Converts a labeled image into a binary image by setting any non-zero value to one.
	 *
	 * @param labelImage Input image. Not modified.
	 * @param binaryImage Output image. Modified.
	 * @return The binary image.
	 */
	public static GrayU8 labelToBinary( GrayS32 labelImage, GrayU8 binaryImage ) {
		binaryImage = InputSanityCheck.declareOrReshape(labelImage, binaryImage, GrayU8.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryImageOps_MT.labelToBinary(labelImage, binaryImage);
		} else {
			ImplBinaryImageOps.labelToBinary(labelImage, binaryImage);
		}

		return binaryImage;
	}

	/**
	 * Only converts the specified blobs over into the binary image
	 *
	 * @param labelImage Input image. Not modified.
	 * @param binaryImage Output image. If null a new one will be declared. Modified.
	 * @param selectedBlobs Each index corresponds to a blob and specifies if it is included or not. Expected
	 * size is the number of found clusters + 1.
	 * @return The binary image.
	 */
	public static GrayU8 labelToBinary( GrayS32 labelImage, GrayU8 binaryImage,
										boolean[] selectedBlobs ) {
		binaryImage = InputSanityCheck.declareOrReshape(labelImage, binaryImage, GrayU8.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplBinaryImageOps_MT.labelToBinary(labelImage, binaryImage, selectedBlobs);
		} else {
			ImplBinaryImageOps.labelToBinary(labelImage, binaryImage, selectedBlobs);
		}

		return binaryImage;
	}

	/**
	 * Only converts the specified blobs over into the binary image. Easier to use version of
	 * {@link #labelToBinary(GrayS32, GrayU8, boolean[])}.
	 *
	 * @param labelImage Input image. Not modified.
	 * @param binaryImage Output image. If null a new one will be declared. Modified.
	 * @param numLabels Number of labels in the image. This is the number of found clusters + 1.
	 * @param selected The index of labels which will be marked as 1 in the output binary image.
	 * @return The binary image.
	 */
	public static GrayU8 labelToBinary( GrayS32 labelImage, GrayU8 binaryImage,
										int numLabels, int... selected ) {
		boolean[] selectedBlobs = new boolean[numLabels];
		for (int i = 0; i < selected.length; i++) {
			selectedBlobs[selected[i]] = true;
		}

		return labelToBinary(labelImage, binaryImage, selectedBlobs);
	}

	/**
	 * Scans through the labeled image and adds the coordinate of each pixel that has been
	 * labeled to a list specific to its label.
	 *
	 * @param labelImage The labeled image.
	 * @param numLabels Number of labeled objects inside the image.
	 * @param queue (Optional) Storage for pixel coordinates. Improves runtime performance. Can be null.
	 * @return List of pixels in each cluster.
	 */
	public static List<List<Point2D_I32>> labelToClusters( GrayS32 labelImage,
														   int numLabels,
														   DogArray<Point2D_I32> queue ) {
		List<List<Point2D_I32>> ret = new ArrayList<>();
		for (int i = 0; i < numLabels + 1; i++) {
			ret.add(new ArrayList<Point2D_I32>());
		}
		if (queue == null) {
			queue = new DogArray<>(numLabels, Point2D_I32::new);
		} else
			queue.reset();

		for (int y = 0; y < labelImage.height; y++) {
			int start = labelImage.startIndex + y*labelImage.stride;
			int end = start + labelImage.width;

			for (int index = start; index < end; index++) {
				int v = labelImage.data[index];
				if (v > 0) {
					Point2D_I32 p = queue.grow();
					p.setTo(index - start, y);
					ret.get(v).add(p);
				}
			}
		}
		// first list is a place holder and should be empty
		if (ret.get(0).size() != 0)
			throw new RuntimeException("BUG!");
		ret.remove(0);
		return ret;
	}

	/**
	 * Sets each pixel in the list of clusters to one in the binary image.
	 *
	 * @param clusters List of all the clusters.
	 * @param binary Output
	 */
	public static void clusterToBinary( List<List<Point2D_I32>> clusters, GrayU8 binary ) {
		ImageMiscOps.fill(binary, 0);

		for (int clusterIdx = 0; clusterIdx < clusters.size(); clusterIdx++) {
			List<Point2D_I32> cluster = clusters.get(clusterIdx);
			for (int pointIdx = 0; pointIdx < cluster.size(); pointIdx++) {
				Point2D_I32 p = cluster.get(pointIdx);
				binary.set(p.x, p.y, 1);
			}
		}
	}

	/**
	 * Several blob rending functions take in an array of colors so that the random blobs can be drawn
	 * with the same color each time. This function selects a random color for each blob and returns it
	 * in an array.
	 *
	 * @param numBlobs Number of blobs found.
	 * @param rand Random number generator
	 * @return array of RGB colors for each blob + the background blob
	 */
	public static int[] selectRandomColors( int numBlobs, Random rand ) {
		int[] colors = new int[numBlobs + 1];
		colors[0] = 0; // black

		int B = 100;

		for (int i = 1; i < colors.length; i++) {
			int c;
			while (true) {
				c = rand.nextInt(0xFFFFFF);
				// make sure its not too dark and can't be distriquished from the background
				if ((c & 0xFF) > B || ((c >> 8) & 0xFF) > B || ((c >> 8) & 0xFF) > B) {
					break;
				}
			}
			colors[i] = c;
		}
		return colors;
	}
}
