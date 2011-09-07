/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.filter.binary.impl;

import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Given a binary image cluster connected image patches together into blobs and uniquely label
 * each blob.  This class contains low level algorithms for performing this task.
 * </p>
 *
 * <p>
 * <ul>
 * <li>quickLabelBlob*: Performs a crude labeling where the same blob will have pixels with multiple
 * numbers.</li>
 * <li>optimizeCoexistTable: Minimizes the number of labels per blob.</li>
 * </p>
 *
 *
 * <p>
 * See: E. R. Davies, "Machine Vision, Theory Algorithms Practicalities" 3rd Ed. Morgan Kauffmann
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplBinaryBlobLabeling {

	/**
	 * Simple but slower algorithm for quickLabel using an 8-connect rule.
	 */
	public static List<LabelNode> quickLabelBlobs8_Naive( ImageUInt8 input , ImageSInt32 _output ) {
		ImageBorder_I32 output = FactoryImageBorder.value(_output,0);
		List<LabelNode> labels = new ArrayList<LabelNode>();

		// add the background node
		labels.add( new LabelNode(0));

		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ ) {
				if( input.get(x,y) == 0 )
					continue;

				 spotQuickLabel8(output, labels, y, x);
			}
		}

		return labels;
	}

	private static void spotQuickLabel8(ImageBorder_I32 output, List<LabelNode> labels, int y, int x) {
		final int p5 = output.get(x-1,y);
		final int p4 = output.get(x-1,y-1);
		final int p3 = output.get(x  ,y-1);
		final int p2 = output.get(x+1,y-1);

		// see if anything around it has been labeled already
		if( 0 == p2+p3+p4+p5 ) {
			int value = labels.size();
			labels.add( new LabelNode(value));
			output.set(x,y,value);
		} else {
			LabelNode l2 = labels.get(p2);
			LabelNode l3 = labels.get(p3);
			LabelNode l4 = labels.get(p4);
			LabelNode l5 = labels.get(p5);
			int value = Math.max(l2.maxIndex,l3.maxIndex);
			value = Math.max(value,l4.maxIndex);
			value = Math.max(value,l5.maxIndex);
			output.set(x,y,value);

			checkConnection(labels, l2, value);
			checkConnection(labels, l3, value);
			checkConnection(labels, l4, value);
			checkConnection(labels, l5, value);
		}
	}

	/**
	 * Simple but slower algorithm for quickLabel using an 8-connect rule.
	 */
	public static List<LabelNode> quickLabelBlobs4_Naive( ImageUInt8 input , ImageSInt32 _output ) {
		ImageBorder_I32 output = FactoryImageBorder.value(_output,0);
		List<LabelNode> labels = new ArrayList<LabelNode>();

		// add the background node
		labels.add( new LabelNode(0));

		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ ) {
				if( input.get(x,y) == 0 )
					continue;

				spotQuickLabel4(output, labels, x, y);
			}
		}

		return labels;
	}

	private static void spotQuickLabel4(ImageBorder_I32 output, List<LabelNode> labels, int x, int y) {
		final int p5 = output.get(x-1,y  );
		final int p3 = output.get(x  ,y-1);

		if( 0 == p3+p5) {
			// nothing around it has been labeled already
			int value = labels.size();
			labels.add( new LabelNode(value) );
			output.set(x,y,value);
		} else {
			// one of the surrounding pixels has been labeled.
			// pick the pixel with the highest ID
			LabelNode l3 = labels.get(p3);
			LabelNode l5 = labels.get(p5);
			int value = Math.max(l3.maxIndex,l5.maxIndex);
			output.set(x,y,value);

			checkConnection(labels, l3, value);
			checkConnection(labels, l5, value);
		}
	}

	private static void checkConnection(List<LabelNode> labels, LabelNode l, int val) {
		if( l.index != 0 && l.maxIndex != val ) {
			l.maxIndex = val;
			l.connections.add(val);
			labels.get(val).connections.add(l.index);
		}
	}

	/**
	 * Faster algorithm for quickLabel using an 8-connect rule.
	 */
	public static List<LabelNode> quickLabelBlobs8( ImageUInt8 input , ImageSInt32 output ) {
		ImageBorder_I32 outputSafe = FactoryImageBorder.value(output,0);
		List<LabelNode> labels = new ArrayList<LabelNode>();

		// add the background node
		labels.add( new LabelNode(0));

		// table the top image
		for( int x = 0; x < input.width; x++ ) {
			if( input.get(x,0) == 0 )
				output.set(x,0,0);
			else
				spotQuickLabel8(outputSafe, labels, 0, x);
		}

		for( int y = 1; y < input.height; y++ ) {

			// label the left border
			if( input.get(0,y) != 0 )
				spotQuickLabel8(outputSafe, labels, y, 0);
			else
				output.set(0,y,0);

			// label the inner portion of the row
			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride + 1;
			int indexEnd = indexIn + input.width - 1;

//			for( int x = 1; x < input.width - 1; x++ ) {
			for( indexIn++; indexIn < indexEnd; indexIn++ , indexOut++ ) {
				if( input.data[indexIn] == 0 ) {
					output.data[indexOut] = 0;
					continue;
				}

				final int p5 = output.data[indexOut-1];
				final int p4 = output.data[indexOut-1-output.stride];
				final int p3 = output.data[indexOut-output.stride];
				final int p2 = output.data[indexOut+1-output.stride];

				// see if anything around it has been labeled
				if( 0 == p2+p3+p4+p5) {
					int value = labels.size();
					labels.add( new LabelNode(value));
					output.data[indexOut] = value;
				} else {
					LabelNode l2 = labels.get(p2);
					LabelNode l3 = labels.get(p3);
					LabelNode l4 = labels.get(p4);
					LabelNode l5 = labels.get(p5);
					int value = Math.max(l2.maxIndex,l3.maxIndex);
					value = Math.max(value,l4.maxIndex);
					value = Math.max(value,l5.maxIndex);
					output.data[indexOut] = value;

					checkConnection(labels, l2, value);
					checkConnection(labels, l3, value);
					checkConnection(labels, l4, value);
					checkConnection(labels, l5, value);
				}
			}

			// label the right border
			if( input.get(input.width - 1,y) != 0 )
				spotQuickLabel8(outputSafe, labels, y, input.width - 1);
			else
				output.set(input.width - 1,y,0);
		}

		return labels;
	}

	/**
	 * Faster algorithm for quickLabel using an 4-connect rule.
	 */
	public static List<LabelNode> quickLabelBlobs4( ImageUInt8 input , ImageSInt32 output )
	{
		ImageBorder_I32 outputSafe = FactoryImageBorder.value(output,0);
		List<LabelNode> labels = new ArrayList<LabelNode>();
		// add the background node
		labels.add( new LabelNode(0));
		
		// table the top image
		for( int x = 0; x < input.width; x++ ) {
			if( input.get(x,0) == 0 ) {
				output.set(x,0,0);
			} else {
				spotQuickLabel4(outputSafe, labels, x, 0);
			}
		}

		for( int y = 1; y < input.height; y++ ) {

			// label the left border
			if( input.get(0,y) != 0 )
				spotQuickLabel4(outputSafe, labels, 0, y);
			else
				output.set(0,y,0);

			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride + 1;
			int indexEnd = indexIn + input.width;

//			for( int x = 1; x < input.width - 1; x++ ) {
			for( indexIn++; indexIn < indexEnd; indexIn++ , indexOut++ ) {
				if( input.data[indexIn] == 0 ) {
					output.data[indexOut] = 0;
					continue;
				}

				final int p5 = output.data[indexOut-1];
				final int p3 = output.data[indexOut-output.stride];

				if( 0 == p3+p5) {
					// nothing around it has been labeled already
					int value = labels.size();
					labels.add( new LabelNode(value) );
					output.data[indexOut] = value;
				} else {
					// one of the surrounding pixels has been labeled.
					// pick the pixel with the highest ID
					LabelNode l3 = labels.get(p3);
					LabelNode l5 = labels.get(p5);
					int value = Math.max(l3.maxIndex,l5.maxIndex);
					output.data[indexOut] = value;

					checkConnection(labels, l3, value);
					checkConnection(labels, l5, value);
				}
			}
		}

		return labels;
	}

	/**
	 * Changes the labels in a binary image.
	 * 
	 * @param input Labeled binary image.
	 * @param convert Lookup table for label conversion.
	 */
	public static void relabelBlobs( ImageSInt32 input , final int convert[] ) {
		for( int y = 0; y < input.height; y++ ) {
			int index = input.startIndex + y*input.stride;
			int end = index+input.width;

			for( ; index < end; index++ ) {
				int val = input.data[index];
				input.data[index] = convert[val];
			}
		}
	}

	public static void optimizeMaxConnect( List<LabelNode> labels )
	{
		boolean change = true;
		while( change ) {
			change = false;
			for( int i = 1; i < labels.size(); i++ ) {
				LabelNode a = labels.get(i);
				for( int j = 0; j < a.connections.size; j++ ) {
					LabelNode b = labels.get( a.connections.data[j] );
					if( a.maxIndex < b.maxIndex ) {
						change = true;
						a.maxIndex = b.maxIndex;
					}
				}
			}
		}
	}

	/**
	 * Makes sure the lowest blob ID numbers are being used.
	 */
	public static int minimizeBlobID( final int maxConnect[] , final int numBlobs )
	{
		int numFound = 0;

		for( int i = 1; i <= numBlobs; i++ ) {
			int val = maxConnect[i];
			if( val == i ) {
				numFound++;
				maxConnect[i] = numFound;
			}
		}

		for( int i = 1; i <= numBlobs; i++ ) {
			int val = maxConnect[i];
			if( val >= i )
				maxConnect[i] = maxConnect[val];
		}

		return numFound;
	}
}
