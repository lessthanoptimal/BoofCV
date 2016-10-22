/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.fiducial;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericFiducialDetectorChecks {

	// tolerance for difference between pixel location and geometric center from reprojection
	protected double pixelAndProjectedTol = 3.0;

	protected List<ImageType> types = new ArrayList<>();

	public abstract ImageBase loadImage(ImageType imageType);

	public abstract FiducialDetector createDetector( ImageType imageType );

	/**
	 * Makes sure the input is not modified
	 */
	@Test
	public void modifyInput() {
		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			ImageBase orig = image.clone();
			FiducialDetector detector = createDetector(type);

			detector.detect(image);

			BoofTesting.assertEquals(image,orig,0);
		}
	}

	@Test
	public void checkMultipleRuns_image() {

		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);

			detector.detect(image);

			assertTrue(detector.totalFound()>= 1);

			Results results = extractResults(detector);

			// run it again
			detector.detect(image);

			// see if it produced exactly the same results
			assertEquals(results.id.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(results.id[i],detector.getId(i));
				Point2D_F64 location = new Point2D_F64();
				detector.getImageLocation(i, location);
				assertTrue(results.pixel.get(0).distance(location) < 1e-4);
			}
		}
	}

	@Test
	public void checkSubImage() {

		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);

			detector.detect(image);

			assertTrue(detector.totalFound()>= 1);

			long foundID[] = new long[ detector.totalFound() ];

			for (int i = 0; i < detector.totalFound(); i++) {
				foundID[i] = detector.getId(i);
			}

			// run it again with a sub-image
			detector.detect(BoofTesting.createSubImageOf(image));

			// see if it produced exactly the same results
			assertEquals(foundID.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(foundID[i],detector.getId(i));
			}
		}
	}


	private Results extractResults( FiducialDetector detector ) {
		Results out = new Results(detector.totalFound());

		for (int i = 0; i < detector.totalFound(); i++) {
			Point2D_F64 pixel = new Point2D_F64();
			detector.getImageLocation(i, pixel);

			out.id[i] = detector.getId(i);
			out.pixel.add(pixel);
		}

		return out;
	}

	private static class Results {
		public long id[];
		public List<Point2D_F64> pixel = new ArrayList<>();

		public Results( int N ) {
			id = new long[ N ];
			pixel = new ArrayList<>();
		}
	}
}
