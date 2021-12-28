/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.drawing.FiducialRenderEngine;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.RectangleLength2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Renders Uchiya Markers
 *
 * @author Peter Abeles
 */
@SuppressWarnings("NullAway.Init")
public class RandomDotMarkerGenerator {

	/** used to draw the fiducial */
	@Getter @Setter protected FiducialRenderEngine render;

	/** Circle's radius. This will be in the same units as markerRegion */
	@Getter @Setter double radius;

	/** Region inside the document that the marker being rendered is specified to be inside of */
	@Getter protected final RectangleLength2D_F64 documentRegion = new RectangleLength2D_F64();

	/** dot locations after being transformed ot fit inside the region */
	@Getter DogArray<Point2D_F64> dotsAdjusted = new DogArray<>(Point2D_F64::new);

	/**
	 * Randomly generates a marker within the allowed region. Ensures that dots do not overlap or touch the marker's
	 * border.
	 *
	 * @param rand Random number generator
	 * @param num Number of dots
	 * @param markerWidth Length of the marker along the x-axis
	 * @param markerHeight Length of the marker along the y-axis
	 * @param dotDiameter The dot's diameter
	 */
	public static List<Point2D_F64> createRandomMarker( Random rand, int num, double markerWidth, double markerHeight, double dotDiameter ) {
		final var points = new ArrayList<Point2D_F64>();
		final var work = new Point2D_F64();

		// Give it a bit of extra space
		double borderCushion = dotDiameter*0.6;

		// Don't want a circle touching the marker's border
		double effectiveWidth = markerWidth - 2*borderCushion;
		double effectiveHeight = markerHeight - 2*borderCushion;

		if (effectiveWidth <= 0 || effectiveHeight <= 0)
			throw new IllegalArgumentException("Marker isn't wide enough for dots to not touch border");

		// The edges of two circle's cant be closer than 1 diameter
		double tol = 2*dotDiameter*dotDiameter;

		int failedAttempts = 0;
		while (failedAttempts < 1000 && points.size() < num) {
			work.x = (rand.nextDouble() - 0.5)*effectiveWidth;
			work.y = (rand.nextDouble() - 0.5)*effectiveHeight;

			// See if there's a point in the list that's too close
			boolean good = true;
			for (int i = 0; i < points.size(); i++) {
				if (points.get(i).distance2(work) < tol) {
					good = false;
					break;
				}
			}

			if (good) {
				points.add(work.copy());
			} else {
				failedAttempts++;
			}
		}

		return points;
	}

	/**
	 * Renders the marker. Automatically scales of offsets to fit inside the document's coordinate system
	 *
	 * @param dots dots on the marker. They should be inside a region -width/2 to width/2.
	 */
	public void render( List<Point2D_F64> dots, double markerWidth, double markerHeight ) {


		// nFind the shift needed to put a point in the center of the draw region
		double regionCenterX = documentRegion.width/2;
		double regionCenterY = documentRegion.height/2;

		// transform from points to paper coordinates
		double point_to_pixel = Math.min(documentRegion.width/markerWidth, documentRegion.height/markerHeight);

		render.init();
		dotsAdjusted.reset();
		for (int dotIdx = 0; dotIdx < dots.size(); dotIdx++) {
			Point2D_F64 p = dots.get(dotIdx);
			Point2D_F64 a = dotsAdjusted.grow();
			a.x = p.x*point_to_pixel + regionCenterX;
			a.y = p.y*point_to_pixel + regionCenterY;
			render.circle(a.x, a.y, radius);
			render.inputToDocument(a.x, a.y, a);
		}
	}
}
