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
import boofcv.struct.image.GrayU8;

/**
 * @author Peter Abeles
 */
public class ContourTracerBase {
	// which connectivity rule is being used. 4 and 8 supported
	protected final ConnectRule rule;
	protected final int ruleN;

	// binary image being traced
	protected GrayU8 binary;

	// coordinate of pixel being examined (x,y)
	protected int x,y;
	// label of the object being traced
	protected int label;
	// direction it moved in
	protected int dir;
	// index of the pixel in the image's internal array
	protected int indexBinary;

	// the pixel index offset to each neighbor
	protected int offsetsBinary[];
	// lookup table for which direction it should search next given the direction it traveled into the current pixel
	protected int nextDirection[];

	/**
	 * Specifies connectivity rule
	 *
	 * @param rule Specifies 4 or 8 as connectivity rule
	 */
	public ContourTracerBase( ConnectRule rule ) {
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
	}

	/**
	 *
	 * @param binary Binary image with a border of zeros added to the outside.
	 */
	public void setInputs(GrayU8 binary ) {
		this.binary = binary;

		if( rule == ConnectRule.EIGHT ) {
			setOffsets8(offsetsBinary,binary.stride);
		} else {
			setOffsets4(offsetsBinary,binary.stride);
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

}
