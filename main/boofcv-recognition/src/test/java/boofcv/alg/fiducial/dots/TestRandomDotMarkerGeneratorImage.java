/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.dots;

import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestRandomDotMarkerGeneratorImage extends BoofStandardJUnit {
	/**
	 * Makes sure the dots are rendered and with the expected properties
	 */
	@Test
	void render() {
		render(200, 200);
		render(200, 180);
	}

	private void render(int width, int height) {
		var alg = new RandomDotMarkerGeneratorImage();

		// Create a set of points at known locations
		// have locations be outside the image to test re-centering
		var points = new ArrayList<Point2D_F64>();
		points.add( new Point2D_F64(-10,10));
		points.add( new Point2D_F64(30,10));
		points.add( new Point2D_F64(-10,50));
		points.add( new Point2D_F64(30,50));
		double markerWidth = 100;

		alg.configure(width,height,40);
		alg.setRadius(7);
		alg.render(points,markerWidth,markerWidth);

		GrayU8 image = alg.getImage();
		assertEquals(width, image.width);
		assertEquals(height, image.height);

//		ShowImages.showBlocking(image,"Tada", 5_000);

		BinaryEllipseDetector<GrayU8> detector = FactoryShapeDetector.ellipse(null, GrayU8.class);

		GrayU8 binary = image.createSameShape();
		ThresholdImageOps.threshold(image, binary, 50, true);
		detector.process(image,binary);

		List<EllipseRotated_F64> found = detector.getFoundEllipses(null);

		assertEquals(points.size(),found.size());

		// sort it by angle
		found.sort((a,b)->{
			double angA = Math.atan2(a.center.y-height/2,a.center.x-width/2);
			double angB = Math.atan2(b.center.y-height/2,b.center.x-width/2);
			return Double.compare(angA,angB);
		});

		double dist01 = found.get(0).center.distance(found.get(1).center);
		double dist12 = found.get(1).center.distance(found.get(2).center);
		double dist23 = found.get(2).center.distance(found.get(3).center);
		double dist30 = found.get(3).center.distance(found.get(0).center);

		assertEquals(dist01, dist12, 2);
		assertEquals(dist01, dist23, 2);
		assertEquals(dist01, dist30, 2);
	}
}
