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

package gecv.alg.filter.binary.impl;

import gecv.core.image.border.FactoryImageBorder;
import gecv.core.image.border.ImageBorder_I32;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;


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

	public static void printCoexist( int [][]coexist , int numBlobs ) {
		for( int i = 1; i <= numBlobs; i++ ) {
			for( int j = 1; j <= numBlobs; j++ ) {
				System.out.printf("%3d ",coexist[i][j]);
			}
			System.out.println();
		}
	}

	/**
	 * Simple but slower algorithm for quickLabel using an 8-connect rule.
	 */
	public static int quickLabelBlobs8_Naive( ImageUInt8 input , ImageSInt32 _output , int coexist[][] ) {
		ImageBorder_I32 output = FactoryImageBorder.value(_output,0);

		int numBlobs = 0;

		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ ) {
				if( input.get(x,y) == 0 )
					continue;

				numBlobs = spotQuickLabel8(output, numBlobs, coexist, y, x);
			}
		}

		return numBlobs;
	}

	private static int spotQuickLabel8(ImageBorder_I32 output, int numBlobs, int[][] coexist, int y, int x) {
		final int p5 = output.get(x-1,y);
		final int p4 = output.get(x-1,y-1);
		final int p3 = output.get(x  ,y-1);
		final int p2 = output.get(x+1,y-1);

		// see if anything around it has been labeled already
		if( 0 == p2+p3+p4+p5 ) {
			numBlobs++;
			output.set(x,y,numBlobs);
			zeroCoexist(coexist,numBlobs);
		} else {
			int val = Math.max(p2,p3);
			val = Math.max(val,p4);
			val = Math.max(val,p5);
			output.set(x,y,val);

			coexist[val][p2] = val;
			coexist[val][p3] = val;
			coexist[val][p4] = val;
			coexist[val][p5] = val;
		}
		return numBlobs;
	}

	/**
	 * Simple but slower algorithm for quickLabel using an 8-connect rule.
	 */
	public static int quickLabelBlobs4_Naive( ImageUInt8 input , ImageSInt32 _output , int coexist[][] ) {
		ImageBorder_I32 output = FactoryImageBorder.value(_output,0);

		int numBlobs = 0;

		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ ) {
				if( input.get(x,y) == 0 )
					continue;

				numBlobs = spotQuickLabel4(output, numBlobs, coexist, y, x);
			}
		}

		return numBlobs;
	}

	private static int spotQuickLabel4(ImageBorder_I32 output, int numBlobs, int[][] coexist, int y, int x) {
		final int p5 = output.get(x-1,y);
		final int p3 = output.get(x  ,y-1);

		// see if anything around it has been labeled already
		if( 0 == p3+p5) {
			numBlobs++;
			output.set(x,y,numBlobs);
			zeroCoexist(coexist,numBlobs);
		} else {
			int val = Math.max(p3,p5);
			output.set(x,y,val);

			coexist[val][p3] = val;
			coexist[val][p5] = val;
		}
		return numBlobs;
	}

	/**
	 * Faster algorithm for quickLabel using an 8-connect rule.
	 */
	public static int quickLabelBlobs8( ImageUInt8 input , ImageSInt32 output , int coexist[][] ) {

		int numBlobs = 0;

		ImageBorder_I32 outputSafe = FactoryImageBorder.value(output,0);

		// table the top image
		for( int x = 0; x < input.width; x++ ) {
			if( input.get(x,0) == 0 )
				continue;

			numBlobs = spotQuickLabel8(outputSafe, numBlobs, coexist, 0, x);
		}

		for( int y = 1; y < input.height; y++ ) {

			// label the left border
			if( input.get(0,y) != 0 )
				numBlobs = spotQuickLabel8(outputSafe, numBlobs, coexist, y, 0);

			// label the inner portion of the row
			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride + 1;
			int indexEnd = indexIn + input.width - 1;

//			for( int x = 1; x < input.width - 1; x++ ) {
			for( indexIn++; indexIn < indexEnd; indexIn++ , indexOut++ ) {
				if( input.data[indexIn] == 0 )
					continue;

				final int p5 = output.data[indexOut-1];
				final int p4 = output.data[indexOut-1-output.stride];
				final int p3 = output.data[indexOut-output.stride];
				final int p2 = output.data[indexOut+1-output.stride];

				// see if anything around it has been labeled
				if( 0 == p2+p3+p4+p5) {
					numBlobs++;
					output.data[indexOut] = numBlobs;
					zeroCoexist(coexist,numBlobs);
				} else {
					int val = Math.max(p2,p3);
					val = Math.max(val,p4);
					val = Math.max(val,p5);
					output.data[indexOut] = val;

					coexist[val][p2] = val;
					coexist[val][p3] = val;
					coexist[val][p4] = val;
					coexist[val][p5] = val;
				}
			}

			// label the right border
			if( input.get(input.width - 1,y) != 0 )
				numBlobs = spotQuickLabel8(outputSafe, numBlobs, coexist, y, input.width - 1);
		}

		return numBlobs;
	}

	/**
	 * Zero the coexist array as needed to save on computations.  This array can be almost
	 * as large as the input image, which can take a bit to zero.
	 */
	private static void zeroCoexist( int coexist[][] , int row ) {
		for( int i = 1; i < row; i++ ) {
			coexist[i][row] = coexist[row][i] = 0;
		}
	}

	/**
	 * Faster algorithm for quickLabel using an 4-connect rule.
	 */
	public static int quickLabelBlobs4( ImageUInt8 input , ImageSInt32 output , int coexist[][] )
	{
		int numBlobs = 0;

		ImageBorder_I32 outputSafe = FactoryImageBorder.value(output,0);

		// table the top image
		for( int x = 0; x < input.width; x++ ) {
			if( input.get(x,0) == 0 )
				continue;

			numBlobs = spotQuickLabel4(outputSafe, numBlobs, coexist, 0, x);
		}

		for( int y = 1; y < input.height; y++ ) {

			// label the left border
			if( input.get(0,y) != 0 )
				numBlobs = spotQuickLabel4(outputSafe, numBlobs, coexist, y, 0);

			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride + 1;
			int indexEnd = indexIn + input.width;

//			for( int x = 1; x < input.width - 1; x++ ) {
			for( indexIn++; indexIn < indexEnd; indexIn++ , indexOut++ ) {
				if( input.data[indexIn] == 0 )
					continue;

				final int p5 = output.data[indexOut-1];
				final int p3 = output.data[indexOut-output.stride];

				// see if anything around it has been labeled
				if( 0 == p3+p5) {
					numBlobs++;
					output.data[indexOut] = numBlobs;
					zeroCoexist(coexist,numBlobs);
				} else {
					int val = Math.max(p3,p5);
					output.data[indexOut] = val;

					coexist[val][p3] = val;
					coexist[val][p5] = val;
				}
			}
		}

		return numBlobs;
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

	/**
	 * Modifies the coexist table to reduce the number of labels per blob down to one.
	 */
	public static void optimizeCoexistTable( final int coexist[][] , final int numBlobs )
	{
		// add diagonals to make computation more efficient
		for( int i = 0; i <= numBlobs; i++ ) {
			coexist[i][i] = i;
			// make it symmetric
			for( int j = i+1; j <= numBlobs; j++ ) {
				coexist[i][j] = coexist[j][i];
			}
		}

		// minimize the rows, can make assumptions about the ordering here
		for( int i = 1; i <= numBlobs; i++ ) {
			int max = -1;
			int[] row = coexist[i];
			for( int j = numBlobs; j >= 1; j-- ) {
				int val = row[j];
				if( val == 0 ) continue;
				if( val > max ) {
					max = val;
				} else {
					row[j] = max;
				}
			}
		}

		boolean changed = true;
		while( changed ) {
			changed = false;

			// minimize the columns
			for( int j = 1; j <= numBlobs; j++ ) {
				int max = -1;
				for( int i = numBlobs; i >= 1; i-- ) {
					int val = coexist[i][j];
					if( val != 0 && val > max ) {
						max = val;
					}
				}

				for( int i = numBlobs; i >= 1; i-- ) {
					int val = coexist[i][j];
					if( val != 0 ) {
						changed |= val != max;
						coexist[i][j] = max;
					}
				}

			}

			// minimize the rows
			for( int i = 1; i <= numBlobs; i++ ) {
				int max = -1;
				int[] row = coexist[i];
				for( int j = numBlobs; j >= 1; j-- ) {
					int val = row[j];
					if( val != 0 && val > max ) {
						max = val;
					}
				}
				for( int j = numBlobs; j >= 1; j-- ) {
					int val = row[j];
					if(  val != 0 ) {
						changed |= val != max;
						row[j] = max;
					}
				}
			}
		}
	}

	/**
	 * Makes sure the lowest blob ID numbers are being used.
	 */
	public static int minimizeBlobID( final int coexist[][] , final int numBlobs ,
									  final int convert[] )
	{
		int numFound = 0;
		int lastValueFound = 0;

		for( int i = 1; i <= numBlobs; i++ ) {
			int val = coexist[i][i];
			if( val != lastValueFound ) {
				numFound++;
				lastValueFound = val;
			}
			convert[i] = numFound;
		}

		return numFound;
	}
}
