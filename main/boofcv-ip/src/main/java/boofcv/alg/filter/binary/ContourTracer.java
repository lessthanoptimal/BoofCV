/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.ConnectRule;
import boofcv.struct.PackedSetsPoint2D_I32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

/**
 * Used to trace the external and internal contours around objects for {@link LinearContourLabelChang2004}.  As it
 * is tracing an object it will modify the binary image by labeling.  The input binary image is assumed to have
 * a 1-pixel border that needs to be compensated for.
 *
 * @author Peter Abeles
 */
public class ContourTracer {

	// Stops saving the contour when it meets or exceeds this value
	private int maxContourSize=Integer.MAX_VALUE;

	// which connectivity rule is being used. 4 and 8 supported
	private final ConnectRule rule;
	private final int ruleN;

	// storage for contour points.
	private PackedSetsPoint2D_I32 storagePoints;

	// binary image being traced
	private GrayU8 binary;
	// label image being marked
	private GrayS32 labeled;

	// coordinate of pixel being examined (x,y)
	private int x,y;
	// label of the object being traced
	private int label;
	// direction it moved in
	private int dir;
	// index of the pixel in the image's internal array
	private int indexBinary;
	private int indexLabel;

	// the pixel index offset to each neighbor
	private int offsetsBinary[];
	private int offsetsLabeled[];
	// lookup table for which direction it should search next given the direction it traveled into the current pixel
	private int nextDirection[];

	/**
	 * Specifies connectivity rule
	 *
	 * @param rule Specifies 4 or 8 as connectivity rule
	 */
	public ContourTracer( ConnectRule rule ) {
		this.rule = rule;

		if( ConnectRule.EIGHT == rule ) {
			// start the next search +2 away from the square it came from
			// the square it came from is the opposite from the previous 'dir'
			nextDirection = new int[8];
			for( int i = 0; i < 8; i++ )
				nextDirection[i] = ((i+4)%8 + 2)%8;
			ruleN = 8;
		} else if( ConnectRule.FOUR == rule ) {
			nextDirection = new int[4];
			for( int i = 0; i < 4; i++ )
				nextDirection[i] = ((i+2)%4 + 1)%4;
			ruleN = 4;
		} else {
			throw new IllegalArgumentException("Connectivity rule must be 4 or 8 not "+rule);
		}

		offsetsBinary = new int[ruleN];
		offsetsLabeled = new int[ruleN];
	}

	/**
	 *
	 * @param binary Binary image with a border of zeros added to the outside.
	 * @param labeled Labeled image.  Size is the same as the original binary image without border.
	 * @param storagePoints
	 */
	public void setInputs(GrayU8 binary , GrayS32 labeled , PackedSetsPoint2D_I32 storagePoints ) {
		this.binary = binary;
		this.labeled = labeled;
		this.storagePoints = storagePoints;

		if( rule == ConnectRule.EIGHT ) {
			setOffsets8(offsetsBinary,binary.stride);
			setOffsets8(offsetsLabeled,labeled.stride);
		} else {
			setOffsets4(offsetsBinary,binary.stride);
			setOffsets4(offsetsLabeled,labeled.stride);
		}
	}

	private void setOffsets8( int offsets[] , int stride ) {
		int s = stride;
		offsets[0] =  1;   // x =  1 y =  0
		offsets[1] =  1+s; // x =  1 y =  1
		offsets[2] =    s; // x =  0 y =  1
		offsets[3] = -1+s; // x = -1 y =  1
		offsets[4] = -1  ; // x = -1 y =  0
		offsets[5] = -1-s; // x = -1 y = -1
		offsets[6] =   -s; // x =  0 y = -1
		offsets[7] =  1-s; // x =  1 y = -1
	}

	private void setOffsets4( int offsets[] , int stride ) {
		int s = stride;
		offsets[0] =  1;   // x =  1 y =  0
		offsets[1] =    s; // x =  0 y =  1
		offsets[2] = -1;   // x = -1 y =  0
		offsets[3] =   -s; // x =  0 y = -1
	}

	/**
	 *
	 * @param label
	 * @param initialX
	 * @param initialY
	 * @param external True for tracing an external contour or false for internal.
	 */
	public void trace( int label , int initialX , int initialY , boolean external )
	{
		int initialDir;
		if( rule == ConnectRule.EIGHT )
			initialDir = external ? 7 : 3;
		else
			initialDir = external ? 0 : 2;

		this.label = label;
		this.dir = initialDir;
		x = initialX;
		y = initialY;

		// index of pixels in the image array
		// binary has a 1 pixel border which labeled lacks, hence the -1,-1 for labeled
		indexBinary = binary.getIndex(x,y);
		indexLabel = labeled.getIndex(x-1,y-1);
		add(x,y);

		// find the next one pixel.  handle case where its an isolated point
		if( !searchOne() ) {
			return;
		} else {
			initialDir = dir;
			moveToNext();
			dir = nextDirection[dir];
		}

		for( ;; ) {
			// search in clockwise direction around the current pixel for next black pixel
			searchOne();
			if( x == initialX && y == initialY && dir == initialDir ) {
				// returned to the initial state again. search is finished
				return;
			} else {
				add(x, y);
				moveToNext();
				dir = nextDirection[dir];
			}
		}
	}

	/**
	 * Searches in a circle around the current point in a clock-wise direction for the first black pixel.
	 */
	private boolean searchOne() {
//		for( int i = 0; i < ruleN; i++ ) {
//			if( checkBlack(indexBinary + offsetsBinary[dir]))
//				return true;
//			dir = (dir+1)%ruleN;
//		}
//		return false;

		// Unrolling here results in about a 10% speed up
		if( ruleN == 4 )
			return searchOne4();
		else
			return searchOne8();
	}

	private boolean searchOne4() {
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%4;
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%4;
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%4;
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%4;
		return false;
	}

	private boolean searchOne8() {
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%8;
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%8;
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%8;
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%8;
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%8;
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%8;
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%8;
		if( checkOne(indexBinary + offsetsBinary[dir]))
			return true;
		dir = (dir+1)%8;
		return false;
	}

	/**
	 * Checks to see if the specified pixel is black (1).  If not the pixel is marked so that it
	 * won't be searched again
	 */
	private boolean checkOne(int index ) {
		if( binary.data[index] == 1 ) {
			return true;
		} else {
			// mark this pixel with a not one value so that it isn't searched again in the future
			binary.data[index] = -1;
			return false;
		}
	}

	private void moveToNext() {
		// move to the next pixel using the precomputed pixel index offsets
		indexBinary += offsetsBinary[dir];
		indexLabel += offsetsLabeled[dir];
		// compute the new pixel coordinate from the binary pixel index
		int a = indexBinary - binary.startIndex;
		x = a%binary.stride;
		y = a/binary.stride;
	}

	/**
	 * Adds a point to the contour list
	 */
	private void add( int x , int y ) {
		labeled.data[indexLabel] = label;
		if( storagePoints.sizeOfTail() < maxContourSize ) {
			storagePoints.addPointToTail(x - 1, y - 1);
		}
	}

	public void setMaxContourSize(int maxContourSize) {
		this.maxContourSize = maxContourSize;
	}

	public ConnectRule getConnectRule() {
		return rule;
	}
}
