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

import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class ContourTracer {

	// storage for contour points.
	FastQueue<Point2D_I32> storagePoints;

	// binary image being traced
	ImageUInt8 binary;
	// label image being marked
	ImageSInt32 labeled;

	// storage for contour
	List<Point2D_I32> contour;

	int x,y,label,dir;

	public void setInputs( ImageUInt8 binary , ImageSInt32 labeled , FastQueue<Point2D_I32> storagePoints ) {
		this.binary = binary;
		this.labeled = labeled;
		this.storagePoints = storagePoints;
	}


	public void trace( int label , int initialX , int initialY , int initialDir , List<Point2D_I32> contour )
	{
		this.label = label;
		this.contour = contour;
		this.dir = initialDir;
		this.x = initialX;
		this.y = initialY;

		add(x,y);

		// find the next black pixel.  handle case where its an isolated point
		if( !searchBlack() ) {
			return;
		} else {
			initialDir = dir;
			moveToNext();
			flipDirection();
			dir = (dir + 2)%8;
		}

		while( true ) {
//			System.out.println("contour.size = "+contour.size());
			// search in clockwise direction around the current pixel for next black pixel
			searchBlack();
//			System.out.println("current "+x+" "+y+" "+dir+" original "+initialX+" "+initialY+" "+initialDir);
			if( x == initialX && y == initialY && dir == initialDir ) {
				// returned to the initial state again. search is finished
//				System.out.println("------------------ Done");
				return;
			}else {
				add(x,y);
				moveToNext();
				flipDirection();
				dir = (dir + 2)%8;
			}
		}
	}

	/**
	 * Searches in a circle around the current point in a clock-wise direction for the first black pixel.
	 */
	// TODO faster if offsets are stored in an array?
	// TODO split into two functions.  one which considers the possibility that its an issolated point and the other
	//      where it does not
	private boolean searchBlack() {
		for( int i = 0; i < 8; i++ ) {
			switch( dir ) {
				case 0:
					if( checkBlack(x+1,y) )
						return true;
					break;

				case 1:
					if( checkBlack(x+1,y+1) )
						return true;
					break;

				case 2:
					if( checkBlack(x,y+1) )
						return true;
					break;

				case 3:
					if( checkBlack(x-1,y+1) )
						return true;
					break;

				case 4:
					if( checkBlack(x-1,y) )
						return true;
					break;

				case 5:
					if( checkBlack(x-1,y-1) )
						return true;
					break;

				case 6:
					if( checkBlack(x,y-1) )
						return true;
					break;

				case 7:
					if( checkBlack(x+1,y-1) )
						return true;
					break;
			}
			dir = (dir+1)%8;
		}
		return false;
	}

	private void flipDirection() {
		switch( dir ) {
			case 0:
				dir = 4;
				break;

			case 1:
				dir = 5;
				break;

			case 2:
				dir = 6;
				break;

			case 3:
				dir = 7;
				break;

			case 4:
				dir = 0;
				break;

			case 5:
				dir = 1;
				break;

			case 6:
				dir = 2;
				break;

			case 7:
				dir = 3;
				break;
		}
	}

	private boolean checkBlack( int x , int y ) {
		// treat pixels outside the image as white
		if( x < 0 || x >= binary.width || y < 0 || y >= binary.height )
			return false;

		int index = binary.getIndex(x,y);

		if( binary.data[index] == 1 ) {
			return true;
		} else {
			// mark white pixels as negative numbers to avoid retracing this contour in the future
			binary.data[index] = -1;
			return false;
		}
	}

	private void moveToNext() {
		switch( dir ) {
			case 0: x++;     break;
			case 1: x++;y++; break;
			case 2:     y++; break;
			case 3: x--;y++; break;
			case 4: x--;     break;
			case 5: x--;y--; break;
			case 6:     y--; break;
			case 7: x++;y--; break;
		}
	}

	/**
	 * Adds a point to the contour list
	 */
	private void add( int x , int y ) {
		Point2D_I32 p = storagePoints.grow();
		p.set(x, y);
		contour.add(p);
		labeled.unsafe_set(x,y,label);
	}
}
