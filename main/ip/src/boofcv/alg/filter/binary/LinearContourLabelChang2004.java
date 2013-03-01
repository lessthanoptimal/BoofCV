/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class LinearContourLabelChang2004 {

	// traces edge pixels
	ContourTracer tracer = new ContourTracer();

	// binary image with a border of zero.
	ImageUInt8 border = new ImageUInt8(1,1);

	// predeclared/recycled data structures
	FastQueue<Point2D_I32> storagePoints = new FastQueue<Point2D_I32>(Point2D_I32.class,true);
	FastQueue<List<Point2D_I32>> storageLists = new FastQueue<List<Point2D_I32>>((Class)ArrayList.class,true);
	FastQueue<Contour> contours = new FastQueue<Contour>(Contour.class,true);

	// internal book keeping variables
	int x,y,indexIn,indexOut;

	public void process( ImageUInt8 binary , ImageSInt32 labeled ) {
		// initialize data structures

		// ensure that the image border pixels are filled with zero by enlarging the image
		if( border.width != binary.width+2 || border.height != binary.height+2)  {
			border.reshape(binary.width + 2, binary.height + 2);
			ImageMiscOps.fillBorder(border, 0, 1);
		}
		border.subimage(1,1,border.width-1,border.height-1).setTo(binary);

		// labeled image must initially be filled with zeros
		ImageMiscOps.fill(labeled,0);

		binary = border;
		storagePoints.reset();
		storageLists.reset();
		contours.reset();
		tracer.setInputs(binary,labeled,storagePoints);

		long totalTrace = 0;
//		long startTime = System.currentTimeMillis();

		// Outside border is all zeros so it can be ignored
		for( y = 1; y < binary.height-1; y++ ) {
			indexIn = binary.startIndex + y*binary.stride+1;
			indexOut = labeled.startIndex + (y-1)*labeled.stride;

			for( x = 1; x < binary.width-1; x++ , indexIn++ , indexOut++) {
				int bit = binary.data[indexIn];

				// white pixels are ignored
				if( !(bit == 1) )
					continue;

				int label = labeled.data[indexOut];

//				long startTraceTime = System.currentTimeMillis();
				if( label == 0 && binary.data[indexIn - binary.stride ] != 1 ) {
//					System.out.println("--------- Step 1 at "+x+" "+y);

					handleStep1();
				} else if( binary.data[indexIn + binary.stride ] == 0 ) {
//					System.out.println("--------- Step 2 at "+x+" "+y);

					handleStep2(labeled, label);
				} else {
//					System.out.println("--------- Step 3 at "+x+" "+y);

					handleStep3(labeled);
				}
//				totalTrace += System.currentTimeMillis()-startTraceTime;

//				System.out.println("Binary");
//				binary.print();
//				System.out.println("Label");
//				labeled.print();
			}
		}

//		long total = System.currentTimeMillis()-startTime;

//		System.out.println("Total elapsed "+total+"  trace = "+totalTrace);
//		System.out.println("Exiting alg");
	}

	public FastQueue<Contour> getContours() {
		return contours;
	}

	/**
	 *  Step 1: If the pixel is unlabeled and the pixel above is white, then it
	 *          must be an external contour of a newly encountered blob.
	 */
	private void handleStep1() {
		Contour c = contours.grow();
		tracer.trace(contours.size(),x,y,7,c.external);
	}

	/**
	 * Step 2: If the pixel below is unmarked and white then it must be an internal contour
	 *         Same behavior it the pixel in question has been labeled or not already
	 */
	private void handleStep2(ImageSInt32 labeled, int label) {
		// if the blob is not labeled and in this state it cannot be against the left side of the image
		if( label == 0 )
			label = labeled.data[indexOut-1];

		Contour c = contours.get(label-1);
		List<Point2D_I32> inner = storageLists.grow();
		inner.clear();
		c.internal.add(inner);
		tracer.trace(label,x,y,3,inner);
	}

	/**
	 * Step 3: Must not be part of the contour but an inner pixel and the pixel to the left must be
	 *         labeled
	 */
	private void handleStep3(ImageSInt32 labeled) {
		if( labeled.data[indexOut] == 0 )
			labeled.data[indexOut] = labeled.data[indexOut-1];
	}

}
