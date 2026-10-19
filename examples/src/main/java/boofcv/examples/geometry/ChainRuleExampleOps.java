/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.geometry;

import georegression.geometry.UtilVector3D_F64;
import georegression.struct.plane.PlaneNormal3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/// Contains various functions useful across examples
public class ChainRuleExampleOps {
	public Random rand = new Random(234);

	/// Generates random points in a square region with extends +/- radius that lie on the plane around
	/// the plane's center point [PlaneNormal3D_F64#p].
	public List<Point3D_F64> createPlane( PlaneNormal3D_F64 plane, double radius, int count ) {
		// two directions inside the plane, so a point is the center plus a mix of them
		Vector3D_F64 axisX = UtilVector3D_F64.perpendicularCanonical(plane.n, null);
		axisX.normalize();
		Vector3D_F64 axisY = plane.n.crossWith(axisX);
		axisY.normalize();

		var points = new ArrayList<Point3D_F64>();
		for (int i = 0; i < count; i++) {
			double a = 2.0*radius*(rand.nextDouble() - 0.5);
			double b = 2.0*radius*(rand.nextDouble() - 0.5);
			points.add(new Point3D_F64(
					plane.p.x + a*axisX.x + b*axisY.x,
					plane.p.y + a*axisX.y + b*axisY.y,
					plane.p.z + a*axisX.z + b*axisY.z));
		}
		return points;
	}

	/// Adds random points in a 3D cube defined by +/- radius.
	public void addOutliers3D( List<Point3D_F64> points, double radius, int count ) {
		for (int i = 0; i < count; i++) {
			points.add(new Point3D_F64(
					2.0*radius*(rand.nextDouble() - 0.5),
					2.0*radius*(rand.nextDouble() - 0.5),
					2.0*radius*(rand.nextDouble() - 0.5)));
		}
	}

	/// Adds gaussian noise to every point
	public void addNoise( List<Point3D_F64> points, double sigma ) {
		for (Point3D_F64 p : points) {
			p.x += rand.nextGaussian()*sigma;
			p.y += rand.nextGaussian()*sigma;
			p.z += rand.nextGaussian()*sigma;
		}
	}

	/// Mean and worst distance from the points to the plane, in the units the points are in
	public static String planeError( List<Point3D_F64> points, PlaneNormal3D_F64 plane ) {
		Vector3D_F64 n = plane.n.copy();
		n.normalize();

		double total = 0.0, max = 0.0;
		for (Point3D_F64 p : points) {
			double d = Math.abs(n.x*(p.x - plane.p.x) + n.y*(p.y - plane.p.y) + n.z*(p.z - plane.p.z));
			total += d;
			max = Math.max(max, d);
		}
		return String.format("mean %7.4f max %7.4f", total/points.size(), max);
	}

	/// A planar calibration target: a grid of points on the z=0 plane, centered on the origin
	public static List<Point3D_F64> createTarget( int cols, int rows, double spacing ) {
		var points = new ArrayList<Point3D_F64>();
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				points.add(new Point3D_F64(spacing*(col - (cols - 1)/2.0),
						spacing*(row - (rows - 1)/2.0), 0.0));
			}
		}
		return points;
	}

	/// Views of the target from a few directions, which is what makes a shared distortion grid
	/// identifiable: an offset field that suits one view will not suit the others
	public static List<Se3_F64> createViews() {
		var views = new ArrayList<Se3_F64>();
		double[][] angles = {{0.0, 0.0}, {0.35, -0.15}, {-0.25, 0.30}, {0.15, 0.28}};
		for (double[] a : angles) {
			var targetToView = new Se3_F64();
			SpecialEuclideanOps_F64.eulerXyz(0.05, -0.03, 1.2, a[0], a[1], 0.1, targetToView);
			views.add(targetToView);
		}
		return views;
	}

	/// Mean and worst distance between what was observed and what a model predicts
	public static String pixelError( List<Point2D_F64> observed, List<Point2D_F64> predicted ) {
		double total = 0.0, max = 0.0;
		for (int i = 0; i < observed.size(); i++) {
			double d = observed.get(i).distance(predicted.get(i));
			total += d;
			max = Math.max(max, d);
		}
		return String.format("mean %7.4f max %7.4f", total/observed.size(), max);
	}
}
