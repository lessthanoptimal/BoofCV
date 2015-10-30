/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib;

import boofcv.alg.fiducial.square.BaseDetectFiducialSquare;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * A fiducial composed of {@link boofcv.alg.fiducial.square.BaseDetectFiducialSquare} intended for use in calibration.
 * It allows parts of the fiducial to be visible and uniquely determined across multiple cameras.  The algorithm
 * simply looks for the expected fiducials in an image and saves he corners that they appear at.  If the same
 * fiducial appears multiple times
 *
 * @author Peter Abeles
 */
public class DetectFiducialSquareGrid<T extends ImageSingleBand> {

	// dimension of grid.  This only refers to black squares and not the white space between
	int numRows;
	int numCols;

	// expected id numbers of each fiducials in row major grid order
	long numbers[];

	BaseDetectFiducialSquare<T> detector;

	FastQueue<Detection> detections = new FastQueue<Detection>(Detection.class,true);

	public DetectFiducialSquareGrid(int numRows, int numCols, long[] numbers,
									BaseDetectFiducialSquare<T> detector)
	{
		this.numRows = numRows;
		this.numCols = numCols;
		this.numbers = numbers;
		this.detector = detector;
	}

	public boolean detect( T input ) {

		detector.process(input);

		FastQueue<FoundFiducial> found = detector.getFound();

		for (int i = 0; i < found.size(); i++) {
			FoundFiducial fid = found.get(i);

			int gridIndex = isExpected(fid.id);
			if( gridIndex >= 0 ) {
				Detection d = lookupDetection(fid.id,gridIndex);
				d.location.set(fid.location);
				d.numDetected++;
			}
		}

		for (int i = detections.size-1; i >= 0; i--) {
			if( detections.get(i).numDetected != 1 ) {
				detections.remove(i);
			}
		}

		return detections.size > 0;
	}

	private int isExpected( long found ) {
		for (int i = 0; i < numbers.length; i++) {
			if( numbers[i] == found ) {
				return i;
			}
		}
		return -1;
	}

	private Detection lookupDetection( long found , int gridIndex) {
		for (int i = 0; i < detections.size(); i++) {
			Detection d = detections.get(i);
			if( d.id == found ) {
				return d;
			}
		}

		Detection d = detections.grow();
		d.reset();
		d.id = found;
		d.gridIndex = gridIndex;

		return d;
	}

	public List<Detection> getDetections() {
		return detections.toList();
	}

	/**
	 * A detected inner fiducial.  Which one, where it is.
	 */
	public static class Detection {
		// number of times it was detected.  Internally used to remove multiple detections
		public int numDetected;
		// location of each detected corner.
		public Quadrilateral_F64 location = new Quadrilateral_F64();
		// the id of the detection
		public long id;
		// where in the grid this detection belongs.
		public int gridIndex;

		public void reset() {
			numDetected = 0;
			id = -1;
			gridIndex = -1;
			location.a.set(0 ,0);
			location.b.set(0, 0);
			location.c.set(0, 0);
			location.d.set(0, 0);
		}
	}
}
