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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.PackedSetsPoint2D_I32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

/**
 * Finds the external contours of binary blobs quickly and efficiently.
 *
 * @author Peter Abeles
 */
// TODO Mark is a contour has holes or not
public class ContourExternalOnly {
	ConnectRule rule = ConnectRule.FOUR;


	int totalLabels = 0;
	Tracer tracer;
	private PackedSetsPoint2D_I32 storagePoints = new PackedSetsPoint2D_I32();
	private FastQueue<Point2D_I32> tmp = new FastQueue<>(Point2D_I32.class,true);

	double timeTracing;

	public ContourExternalOnly() {
		tracer = new Tracer(rule);
	}

	public void process(GrayU8 binary , GrayS32 labeled) {
		storagePoints.reset();
		tmp.reset();
		ImageMiscOps.fillBorder(binary, 0, 1);

		labeled.reshape(binary.width-2,binary.height-2);
		ImageMiscOps.fill(labeled,-1);

		tracer.setInputs(binary,labeled);

		timeTracing = 0;

		// inside of the label it's inside of
		int inside;
		final byte binaryData[] = binary.data;
		final int labeledData[] = labeled.data;

		for (int y = 1; y < binary.height-1; y++) {
			int x = 1;
			int indexBinary = binary.startIndex + y*binary.stride + 1;
			int indexLabeled = labeled.startIndex + (y-1)*labeled.stride ;

			int end = indexBinary + binary.width - 2;

			while( true ) {
				int delta = findNotZero(binaryData, indexBinary, end) - indexBinary;
				indexBinary += delta;
				if (indexBinary == end)
					break;
				indexLabeled += delta;
				x += delta;

//				System.out.println("y = "+y+"  delta not zero "+delta);

				// If this pixel has NOT already been labeled then trace until it runs into a labeled pixel or it
				// completes the trace. If a labeled pixel is not encountered then it must be an external contour

				if( binaryData[indexBinary] == 1 ) {
					if( tracer.trace(x,y,true) ) {
						// it completed a loop without stumbling into an existing contour. Must be a new blob
						// and an external contour
						inside = totalLabels++;
//						System.out.println("  Trace External Length = "+storagePoints.sizeOfTail()+" label="+inside);
						saveToLabel(binary,labeled,inside);
					} else {
						inside = labeled.unsafe_get(tracer.x-1,tracer.y-1);
//						System.out.println("  Trace reconnected. Label = "+inside+" length = "+storagePoints.sizeOfTail());
						saveToLabel(binary,labeled,inside);
						storagePoints.removeTail();
					}

				} else {
					// labeled already. Figure out what it was labeled as
					inside = labeledData[indexLabeled];
//					System.out.println("  Already labeled contour. label = "+inside);
				}

				// It's now inside a ones blob. Move forward until it hits a 0 pixel
				delta = findZero(binaryData, indexBinary, end) - indexBinary;
				indexBinary += delta;
				if (indexBinary == end)
					break;
				indexLabeled += delta;
				x += delta;

//				System.out.println("y = "+y+"  delta zero "+delta);

				// If this pixel has NOT already been labeled trace until it completes a loop or it encounters a
				// labeled pixel. This is always an internal contour
				byte v = binaryData[indexBinary-1];
				if( v == 1 ) {
					tracer.trace(x-1,y,false);
//					System.out.println("  Trace Internal Length = "+storagePoints.sizeOfTail());
					saveToLabel(binary,labeled,inside);
				} else if( v == -1 ) {
//					System.out.println("  Already traced. Inside of "+inside);
					// sanity check, should be exiting this blob and so the inner contour should have the same label
					if( labeled.unsafe_get(x-2,y-1) != inside ) {
						throw new RuntimeException("Impossible. Found="+labeled.unsafe_get(x-1,y-1));
					}
				}
			}
		}
//		System.out.println("Total tracing "+timeTracing+" (ms)");
	}

	void saveToLabel( GrayU8 binary, GrayS32 labeled, int label ) {
		storagePoints.getSet(storagePoints.size()-1,tmp);
		for (int i = 0; i < tmp.size; i++) {
			Point2D_I32 p = tmp.get(i);
			labeled.unsafe_set(p.x,p.y,label);
			binary.unsafe_set(p.x+1,p.y+1,-1);
		}
	}

	static int findNotZero( byte[] data , int index , int end ) {
		while( index < end && data[index] == 0 ) {
			index++;
		}
		return index;
	}

	static  int findZero( byte[] data , int index , int end ) {
		while( index < end && data[index] != 0 ) {
			index++;
		}
		return index;
	}


	@SuppressWarnings("Duplicates")
	class Tracer extends ContourTracerBase {

		public Tracer(ConnectRule rule) {
			super(rule);
		}

		public boolean trace( int initialX , int initialY , boolean external )
		{
//			long before = System.nanoTime();
			boolean ret = traceA(initialX, initialY, external);
//			long after = System.nanoTime();

//			timeTracing += (after-before)*1e-6;
			return ret;
		}

		public boolean traceA( int initialX , int initialY , boolean external )
		{
			// start a contour here
			storagePoints.grow();
			int initialDir;
			if( rule == ConnectRule.EIGHT )
				initialDir = external ? 7 : 3;
			else
				initialDir = external ? 0 : 2;

			this.dir = initialDir;
			x = initialX;
			y = initialY;

			// index of pixels in the image array
			// binary has a 1 pixel border which labeled lacks, hence the -1,-1 for labeled
			indexBinary = binary.getIndex(x,y);
			add(x,y);

			// find the next one pixel.  handle case where its an isolated point
			if( !searchNotZero() ) {
				return true;
			} else {
				initialDir = dir;
				moveToNext();
				dir = nextDirection[dir];
			}

			while( binary.data[indexBinary] != -1 ) {
				// search in clockwise direction around the current pixel for next black pixel
//				searchNotZero();
				searchNotOne4();

				if( x == initialX && y == initialY && dir == initialDir ) {
					// returned to the initial state again. search is finished
					return true;
				} else {
					add(x, y);
					moveToNext();
					dir = nextDirection[dir];
				}
			}

			// It stumbled into a pre-existing contour
			return false;
		}

		/**
		 * Searches in a circle around the current point in a clock-wise direction for the first black pixel.
		 */
		private boolean searchNotZero() {
			// Unrolling here results in about a 10% speed up
			if( ruleN == 4 )
				return searchNotOne4();
			else
				return searchNotOne8();
		}

		private boolean searchNotOne4() {
//			if (binary.data[indexBinary + offsetsBinary[dir]] == 0) {
//				dir = (dir+1)%4;
//				if (binary.data[indexBinary + offsetsBinary[dir]] == 0) {
//					dir = (dir+1)%4;
//					if (binary.data[indexBinary + offsetsBinary[dir]] == 0) {
//						dir = (dir+1)%4;
//						if (binary.data[indexBinary + offsetsBinary[dir]] == 0) {
//							return false;
//						}
//						return true;
//					}
//					return true;
//				}
//				return true;
//			}
//			return true;

			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%4;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%4;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%4;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%4;
			return false;
		}

		private boolean searchNotOne8() {
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			return false;
		}


		private boolean checkNotZero(int index ) {
			return binary.data[index] != 0;
		}

		private void moveToNext() {
			// move to the next pixel using the precomputed pixel index offsets
			indexBinary += offsetsBinary[dir];
			// compute the new pixel coordinate from the binary pixel index
			int a = indexBinary - binary.startIndex;
			x = a%binary.stride;
			y = a/binary.stride;
		}

		/**
		 * Adds a point to the contour list
		 */
		private void add( int x , int y ) {
//			System.out.println("    $ trace "+x+" "+y);
			binary.data[indexBinary] = -2;
			storagePoints.addPointToTail(x - 1, y - 1);
		}
	}

	public PackedSetsPoint2D_I32 getExternalContours() {
		return storagePoints;
	}
}
