/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.binary.impl.ImplBinaryBlobLabeling;
import boofcv.alg.filter.binary.impl.ImplBinaryBorderOps;
import boofcv.alg.filter.binary.impl.ImplBinaryInnerOps;
import boofcv.alg.filter.binary.impl.LabelNode;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.ImageBorderValue;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

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
 * DESIGN NOTE: 8-bit integer images ({@link ImageUInt8}) are used instead of images composed of boolean values because
 * there is no performance advantage.  According to the virtual machines specification binary arrays are stored as
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
	 * @param output Output image. Can be same as either input.  If null a new instance will be declared, Modified.
	 * @return Output of logical operation.
	 */
	public static ImageUInt8 logicAnd( ImageUInt8 inputA , ImageUInt8 inputB , ImageUInt8 output )
	{
		InputSanityCheck.checkSameShape(inputA,inputB);
		output = InputSanityCheck.checkDeclare(inputA, output);

		for( int y = 0; y < inputA.height; y++ ) {
			int indexA = inputA.startIndex + y*inputA.stride;
			int indexB = inputB.startIndex + y*inputB.stride;
			int indexOut = output.startIndex + y*output.stride;

			int end = indexA + inputA.width;
			for( ; indexA < end; indexA++,indexB++,indexOut++) {
				int valA = inputA.data[indexA];
				output.data[indexOut] = valA == 1 && valA == inputB.data[indexB] ? (byte)1 : (byte)0;
			}
		}

		return output;
	}

	/**
	 * For each pixel it applies the logical 'or' operator between two images.
	 *
	 * @param inputA First input image. Not modified.
	 * @param inputB Second input image. Not modified.
	 * @param output Output image. Can be same as either input.  If null a new instance will be declared, Modified.
	 * @return Output of logical operation.
	 */
	public static ImageUInt8 logicOr( ImageUInt8 inputA , ImageUInt8 inputB , ImageUInt8 output )
	{
		InputSanityCheck.checkSameShape(inputA,inputB);
		output = InputSanityCheck.checkDeclare(inputA, output);

		for( int y = 0; y < inputA.height; y++ ) {
			int indexA = inputA.startIndex + y*inputA.stride;
			int indexB = inputB.startIndex + y*inputB.stride;
			int indexOut = output.startIndex + y*output.stride;

			int end = indexA + inputA.width;
			for( ; indexA < end; indexA++,indexB++,indexOut++) {
				output.data[indexOut] =
						inputA.data[indexA] == 1 ||  1 == inputB.data[indexB] ? (byte)1 : (byte)0;
			}
		}

		return output;
	}

	/**
	 * For each pixel it applies the logical 'xor' operator between two images.
	 *
	 * @param inputA First input image. Not modified.
	 * @param inputB Second input image. Not modified.
	 * @param output Output image. Can be same as either input.  If null a new instance will be declared, Modified.
	 * @return Output of logical operation.
	 */
	public static ImageUInt8 logicXor( ImageUInt8 inputA , ImageUInt8 inputB , ImageUInt8 output )
	{
		InputSanityCheck.checkSameShape(inputA,inputB);
		output = InputSanityCheck.checkDeclare(inputA, output);

		for( int y = 0; y < inputA.height; y++ ) {
			int indexA = inputA.startIndex + y*inputA.stride;
			int indexB = inputB.startIndex + y*inputB.stride;
			int indexOut = output.startIndex + y*output.stride;

			int end = indexA + inputA.width;
			for( ; indexA < end; indexA++,indexB++,indexOut++) {
				output.data[indexOut] = inputA.data[indexA] != inputB.data[indexB] ? (byte)1 : (byte)0;
			}
		}

		return output;
	}

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

		ImplBinaryInnerOps.erode4(input, output);
		ImplBinaryBorderOps.erode4(input, output);

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

		ImplBinaryInnerOps.dilate4(input, output);
		ImplBinaryBorderOps.dilate4(input, output);

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

		ImplBinaryInnerOps.edge4(input, output);
		ImplBinaryBorderOps.edge4(input, output);

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

		ImplBinaryInnerOps.erode8(input, output);
		ImplBinaryBorderOps.erode8(input, output);

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

		ImplBinaryInnerOps.dilate8(input, output);
		ImplBinaryBorderOps.dilate8(input, output);

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

		ImplBinaryInnerOps.edge8(input, output);
		ImplBinaryBorderOps.edge8(input, output);

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

		ImplBinaryInnerOps.removePointNoise(input, output);
		ImplBinaryBorderOps.removePointNoise(input, output);

		return output;
	}

	/**
	 * <p>
	 * Connects pixels together using an 8-connect rule.  Each cluster of connected pixels (a blob)
	 * is given a unique number >= 1.
	 * </p>
	 *
	 * <p>
	 * The coexist table is used internally to store intermediate results.  It is used supplied because
	 * pre-allocating the array was shown to improve performance about 5 times.
	 * </p>
	 *
	 * @param input Binary input image.
	 * @param output The labeled blob image. Modified.
	 * @return How many blobs were found.
	 */
	public static int labelBlobs8( ImageUInt8 input , ImageSInt32 output )
	{
		InputSanityCheck.checkSameShape(input,output);

		List<LabelNode> labels = ImplBinaryBlobLabeling.quickLabelBlobs8(input,output);
		ImplBinaryBlobLabeling.optimizeMaxConnect(labels);
		int blobs[] = new int[ labels.size() ]; // todo clean this up
		for( int i = 0; i < blobs.length; i++ ) {
			blobs[i] = labels.get(i).maxIndex;
		}
		int newNumBlobs = ImplBinaryBlobLabeling.minimizeBlobID(blobs,blobs.length-1);
		ImplBinaryBlobLabeling.relabelBlobs(output,blobs);

		return newNumBlobs;
	}


	/**
	 * <p>
	 * Connects pixels together using an 4-connect rule.  Each cluster of connected pixels (a blob)
	 * is given a unique number >= 1.
	 * </p>
	 *
	 * <p>
	 * The coexist table is used internally to store intermediate results.  It is used supplied because
	 * pre-allocating the array was shown to improve performance about 5 times.
	 * </p>
	 *
	 * @param input Binary input image. Not modified.
	 * @param output The labeled blob image. Modified.
	 * @return How many blobs were found.
	 */
	public static int labelBlobs4( ImageUInt8 input , ImageSInt32 output )
	{
		InputSanityCheck.checkSameShape(input,output);

		List<LabelNode> labels = ImplBinaryBlobLabeling.quickLabelBlobs4(input,output);
		ImplBinaryBlobLabeling.optimizeMaxConnect(labels);
		int blobs[] = new int[ labels.size() ]; // todo clean this up
		for( int i = 0; i < blobs.length; i++ ) {
			blobs[i] = labels.get(i).maxIndex;
		}
		int newNumBlobs = ImplBinaryBlobLabeling.minimizeBlobID(blobs,blobs.length-1);
		ImplBinaryBlobLabeling.relabelBlobs(output,blobs);

		return newNumBlobs;
	}

	/**
	 * Used to change the labels in a labeled binary image.
	 *
	 * @param image Labeled binary image.
	 * @param labels Look up table where the indexes are the current label and the value are its new value.
	 */
	public static void relabel( ImageSInt32 image , int labels[] ) {
		ImplBinaryBlobLabeling.relabelBlobs(image,labels);
	}

	/**
	 * Converts a labeled image into a binary image by setting any non-zero value to one.
	 *
	 * @param labelImage Input image. Not modified.
	 * @param binaryImage Output image. Modified.
	 * @return The binary image.
	 */
	public static ImageUInt8 labelToBinary( ImageSInt32 labelImage , ImageUInt8 binaryImage ) {
		binaryImage = InputSanityCheck.checkDeclare(labelImage, binaryImage, ImageUInt8.class);

		for( int y = 0; y < labelImage.height; y++ ) {

			int indexIn = labelImage.startIndex + y*labelImage.stride;
			int indexOut = binaryImage.startIndex + y*binaryImage.stride;

			int end = indexIn + labelImage.width;

			for( ; indexIn < end; indexIn++, indexOut++ ) {
				if( 0 == labelImage.data[indexIn] ) {
					binaryImage.data[indexOut] = 0;
				} else {
					binaryImage.data[indexOut] = 1;
				}
			}
		}

		return binaryImage;
	}

	/**
	 * Only converts the specified blobs over into the binary image
	 *
	 * @param labelImage Input image. Not modified.
	 * @param binaryImage Output image. Modified.
	 * @param selectedBlobs Each index corresponds to a blob and specifies if it is included or not.
	 * @return The binary image.
	 */
	public static ImageUInt8 labelToBinary( ImageSInt32 labelImage , ImageUInt8 binaryImage ,
											boolean selectedBlobs[] )
	{
		binaryImage = InputSanityCheck.checkDeclare(labelImage, binaryImage, ImageUInt8.class);

		for( int y = 0; y < labelImage.height; y++ ) {

			int indexIn = labelImage.startIndex + y*labelImage.stride;
			int indexOut = binaryImage.startIndex + y*binaryImage.stride;

			int end = indexIn + labelImage.width;

			for( ; indexIn < end; indexIn++, indexOut++ ) {
				int val = labelImage.data[indexIn];
				if( 0 == val ) {
					binaryImage.data[indexOut] = 0;
				} else {
					if( selectedBlobs[val] ) {
						binaryImage.data[indexOut] = 1;
					} else {
						binaryImage.data[indexOut] = 0;
					}

				}
			}
		}

		return binaryImage;
	}

	/**
	 * Scans through the labeled image and adds the coordinate of each pixel that has been
	 * labeled to a list specific to its label.
	 *
	 * @param labelImage The labeled image.
	 * @param numLabels Number of labeled objects inside the image.
	 * @param queue Predeclare returned points.  Improves runtime performance. Can be null.
	 * @return List of pixels in each cluster.
	 */
	public static List<List<Point2D_I32>> labelToClusters( ImageSInt32 labelImage ,
														   int numLabels ,
														   FastQueue<Point2D_I32> queue )
	{
		List<List<Point2D_I32>> ret = new ArrayList<List<Point2D_I32>>();
		for( int i = 0; i < numLabels+1; i++ ) {
			ret.add( new ArrayList<Point2D_I32>() );
		}
		if( queue == null ) {
			queue = new FastQueue<Point2D_I32>(numLabels,Point2D_I32.class,true);
		} else
			queue.reset();

		for( int y = 0; y < labelImage.height; y++ ) {
			int start = labelImage.startIndex + y*labelImage.stride;
			int end = start + labelImage.width;

			for( int index = start; index < end; index++ ) {
				int v = labelImage.data[index];
				if( v > 0 ) {
					Point2D_I32 p = queue.grow();
					p.set(index-start,y);
					ret.get(v).add(p);
				}
			}
		}
		// first list is a place holder and should be empty
		if( ret.get(0).size() != 0 )
			throw new RuntimeException("BUG!");
		ret.remove(0);
		return ret;
	}

	/**
	 * Extracts edges from a labeled blob image using a 4-connect rule
	 *
	 * @param labelImage labeled image
	 * @param numLabels Number of labels in the image
	 * @param queue Optional queue with predeclared points.  Can be null.
	 * @return List of found contours around labeled image.
	 */
	public static List<List<Point2D_I32>> labelEdgeCluster4( ImageSInt32 labelImage ,
															 int numLabels ,
															 FastQueue<Point2D_I32> queue )
	{
		List<List<Point2D_I32>> ret = new ArrayList<List<Point2D_I32>>();
		for( int i = 0; i < numLabels+1; i++ ) {
			ret.add( new ArrayList<Point2D_I32>() );
		}
		if( queue == null ) {
			queue = new FastQueue<Point2D_I32>(numLabels,Point2D_I32.class,true);
		} else
			queue.reset();

		// todo this method is slow and should be speed up with an inner and outer version
		ImageBorder_I32 in = ImageBorderValue.wrap(labelImage, 1);
		
		for( int y = 0; y < labelImage.height; y++ ) {

			for( int x = 0; x < labelImage.width; x++ ) {
				int v = in.get(x,y);
				if( v == 0 )
					continue;
				// see if any part of it (4-connect) is touching a non-member pixel
				if( in.get(x-1,y) == 0 || in.get(x+1,y) == 0 || in.get(x,y-1) == 0 || in.get(x,y+1) == 0 ) {
					Point2D_I32 p = queue.grow();
					p.set(x,y);
					ret.get(v).add(p);
				}
			}
		}
		// first list is a place holder and should be empty
		if( ret.get(0).size() != 0 )
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
	public static void clusterToBinary( List<List<Point2D_I32>> clusters ,
										ImageUInt8 binary )
	{
		ImageMiscOps.fill(binary, 0);
		
		for( List<Point2D_I32> l : clusters ) {
			for( Point2D_I32 p : l ) {
				binary.set(p.x,p.y,1);
			}
		}
	}
	
	/**
	 * Several blob rending functions take in an array of colors so that the random blobs can be drawn
	 * with the same color each time.  This function selects a random color for each blob and returns it
	 * in an array.
	 * @param numBlobs  Number of blobs found.
	 * @param rand Random number generator
	 * @return array of RGB colors for each blob + the background blob
	 */
	public static int[] selectRandomColors( int numBlobs , Random rand ) {
		int colors[] = new int[ numBlobs+1 ];
		colors[0] = 0; // black

		int B = 100;

		for( int i = 1; i < colors.length; i++ ) {
			int c;
			while( true ) {
				c = rand.nextInt(0xFFFFFF);
				// make sure its not too dark and can't be distriquished from the background
				if( (c & 0xFF) > B || ((c >> 8) & 0xFF) > B || ((c >> 8 ) & 0xFF) > B ) {
					break;
				}
			}
			colors[i] = c;
		}
		return colors;
	}
}
