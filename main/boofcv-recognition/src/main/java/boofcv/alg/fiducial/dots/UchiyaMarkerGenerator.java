/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.RectangleLength2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.abs;
import static java.lang.Math.max;

/**
 * Renders Uchiya Markers
 *
 * @author Peter Abeles
 */
public class UchiyaMarkerGenerator {

	// used to draw the fiducial
	@Getter protected FiducialRenderEngine render;

	// Circle's radius. This will be in the same units as markerRegion
	@Getter @Setter double radius;

	// Region insid ethe document that the marker being rendered is specified to be inside of
	@Getter protected final RectangleLength2D_F64 documentRegion = new RectangleLength2D_F64();

	// dot locations after being transformed ot fit inside the region
	@Getter FastQueue<Point2D_F64> dotsAdjusted = new FastQueue<>(Point2D_F64::new);

	//----------------- workspace variables
	private Point2D_F64 average = new Point2D_F64();

	/**
	 * Randomly generates a marker within the allowed region. Ensures tat there is sufficient spacing between
	 * points
	 */
	public static List<Point2D_F64> createRandomMarker(Random rand , int num , double regionWidth , double minDistance ) {
		final var points = new ArrayList<Point2D_F64>();
		final var work = new Point2D_F64();

		double tol = minDistance*minDistance;

		int failedAttempts = 0;
		while( failedAttempts < 100 && points.size() < num ) {
			work.x = rand.nextDouble()*regionWidth;
			work.y = rand.nextDouble()*regionWidth;

			// See if there's a point in the list that's too close
			boolean good = true;
			for (int i = 0; i < points.size(); i++) {
				if( points.get(i).distance2(work) < tol ) {
					good = false;
					break;
				}
			}

			if( good ) {
				points.add(work.copy());
			} else {
				failedAttempts++;
			}
		}

		return points;
	}

	/**
	 * Renders the marker. Automatically scales of offsets to fit inside the document's coordinate system
	 */
	public void render( List<Point2D_F64> dots ) {
		UtilPoint2D_F64.mean(dots, average);
		double maxDistance = 0;
		for ( var p : dots ) {
			maxDistance = max(maxDistance, abs(p.x-average.x));
			maxDistance = max(maxDistance, abs(p.y-average.y));
		}
		if( maxDistance == 0.0 )
			maxDistance = 1.0;

		// The length of the square the circle's centers can appear inside of
		double drawLength = Math.min(documentRegion.width,documentRegion.height)-2*radius;

		// nFind the shift needed to put a point in the center of the draw region
		double regionCenterX = documentRegion.width/2;
		double regionCenterY = documentRegion.height/2;

		// transform from points to paper coordinates
		double point_to_pixel = drawLength/(2.0*maxDistance);

		render.init();
		dotsAdjusted.reset();
		for( var p : dots ) {
			Point2D_F64 a = dotsAdjusted.grow();
			a.x = (p.x - average.x)*point_to_pixel + regionCenterX;
			a.y = (p.y - average.y)*point_to_pixel + regionCenterY;
			render.circle(a.x,a.y,radius);
		}
	}
}
