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

package boofcv.abst.geo.bundle;

import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.geo.bundle.cameras.BundleDummyCamera;
import boofcv.alg.geo.bundle.cameras.BundlePinhole;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Base class for implementations of {@link SceneStructure}. Contains data structures
 * common to all implementations
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class SceneStructureCommon implements SceneStructure {
	public final DogArray<Camera> cameras = new DogArray<>(Camera::new, Camera::reset);
	public DogArray<Point> points;
	/** True if homogenous coordinates are being used */
	protected boolean homogenous;
	/** number of elements in a point. Will be 3 or 4 */
	protected @Getter int pointSize;

	protected SceneStructureCommon( boolean homogenous ) {
		setHomogenous(homogenous);
	}

	/**
	 * Used to change if homogenous coordinates are used or not. All past points are discarded when this function is
	 * called
	 *
	 * @param homogenous true for homogenous coordinates or false for 3D cartesian
	 */
	public void setHomogenous( boolean homogenous ) {
		this.homogenous = homogenous;
		pointSize = homogenous ? 4 : 3;
		points = new DogArray<>(() -> new Point(pointSize), Point::reset);
	}

	/**
	 * Specifies the location of a point in 3D space
	 *
	 * @param which Which point is being specified
	 * @param x coordinate along x-axis
	 * @param y coordinate along y-axis
	 * @param z coordinate along z-axis
	 */
	public void setPoint( int which, double x, double y, double z ) {
		points.data[which].set(x, y, z);
	}

	/**
	 * Specifies the location of a point as a 3D homogenous coordinate
	 *
	 * @param which Which point is being specified
	 * @param x coordinate along x-axis
	 * @param y coordinate along y-axis
	 * @param z coordinate along z-axis
	 * @param w w-coordinate
	 */
	public void setPoint( int which, double x, double y, double z, double w ) {
		points.data[which].set(x, y, z, w);
	}

	/**
	 * Specifies the camera model being used.
	 *
	 * @param which Which camera is being specified
	 * @param fixed If these parameters are constant or not
	 * @param model The camera model
	 */
	public void setCamera( int which, boolean fixed, BundleAdjustmentCamera model ) {
		cameras.get(which).known = fixed;
		cameras.get(which).model = model;
	}

	public void setCamera( int which, boolean fixed, CameraPinhole intrinsic ) {
		setCamera(which, fixed, BundleAdjustmentOps.convert(intrinsic, (BundlePinhole)null));
	}

	public void setCamera( int which, boolean fixed, CameraPinholeBrown intrinsic ) {
		setCamera(which, fixed, BundleAdjustmentOps.convert(intrinsic, (BundlePinholeBrown)null));
	}

	/**
	 * Specifies that the point was observed in this view.
	 *
	 * @param pointIndex index of point
	 * @param viewIndex index of view
	 */
	public void connectPointToView( int pointIndex, int viewIndex ) {
		Point p = points.data[pointIndex];

		if (p.views.contains(viewIndex))
			throw new IllegalArgumentException("Tried to add the same view twice. viewIndex=" + viewIndex);

		p.views.add(viewIndex);
	}

	public DogArray<Point> getPoints() {
		return points;
	}

	public DogArray<Camera> getCameras() {
		return cameras;
	}

	public <T extends BundleAdjustmentCamera> T getCameraModel( int cameraIndex ) {
		return Objects.requireNonNull(cameras.get(cameraIndex).getModel());
	}

	/**
	 * Counts the total number of unknown camera parameters that will be optimized/
	 *
	 * @return Number of parameters
	 */
	public int getUnknownCameraParameterCount() {
		int total = 0;
		for (int i = 0; i < cameras.size; i++) {
			if (!cameras.data[i].known) {
				total += cameras.data[i].model.getIntrinsicCount();
			}
		}
		return total;
	}

	/**
	 * Removes the points specified in 'which' from the list of points. 'which' must be ordered
	 * from lowest to highest index.
	 *
	 * @param which Ordered list of point indexes to remove
	 */
	public void removePoints( DogArray_I32 which ) {
		points.remove(which.data, 0, which.size, null);
	}

	/**
	 * Camera which is viewing the scene. Contains intrinsic parameters.
	 */
	public static class Camera {
		/**
		 * If the parameters are assumed to be known and should not be optimised.
		 */
		public boolean known = true;
		public BundleAdjustmentCamera model = BundleDummyCamera.INSTANCE;

		public <T extends BundleAdjustmentCamera> @Nullable T getModel() {
			return (T)model;
		}

		public void reset() {
			known = true;
			model = BundleDummyCamera.INSTANCE;
		}

		public boolean isIdentical( Camera m, double tol ) {
			if (known != m.known)
				return false;
			if (model == null)
				return m.model == BundleDummyCamera.INSTANCE;
			else if (m.model == BundleDummyCamera.INSTANCE)
				return false;

			try {
				if (model instanceof BundlePinholeSimplified) {
					BundlePinholeSimplified a = (BundlePinholeSimplified)model;
					BundlePinholeSimplified b = (BundlePinholeSimplified)m.model;
					return a.isIdentical(b, tol);
				} else {
					throw new RuntimeException("Add support for " + model.getClass().getSimpleName());
				}
			} catch (ClassCastException e) {
				return false;
			}
		}
	}

	public static class Point {

		/**
		 * Where the point is in the world reference frame. 3D or 4D space
		 */
		public final double[] coordinate;

		/**
		 * Indexes of the views that this point appears in
		 */
		public DogArray_I32 views = new DogArray_I32(0);

		public Point( int dof ) {
			coordinate = new double[dof];
		}

		public void reset() {
			views.reset();
			coordinate[0] = Double.NaN;
		}

		/**
		 * Removes the specified view from the list of views. If it's not contained in the list
		 * an exception is thrown
		 *
		 * @param which Index of the view which is to be removed
		 */
		public void removeView( int which ) {
			int index = views.indexOf(which);
			if (index == -1)
				throw new RuntimeException("BUG. Could not find in list of views. which=" + which);
			views.remove(index);
		}

		public void set( double x, double y, double z ) {
			coordinate[0] = x;
			coordinate[1] = y;
			coordinate[2] = z;
		}

		public void set( double x, double y, double z, double w ) {
			coordinate[0] = x;
			coordinate[1] = y;
			coordinate[2] = z;
			coordinate[3] = w;
		}

		public void get( Point3D_F64 p ) {
			p.x = coordinate[0];
			p.y = coordinate[1];
			p.z = coordinate[2];
		}

		public void get( Point4D_F64 p ) {
			p.x = coordinate[0];
			p.y = coordinate[1];
			p.z = coordinate[2];
			p.w = coordinate[3];
		}

		public double getX() {
			return coordinate[0];
		}

		public double getY() {
			return coordinate[1];
		}

		public double getZ() {
			return coordinate[2];
		}

		public double getW() {
			return coordinate[3];
		}

		public double distanceSq( Point3D_F64 p ) {
			double dx = coordinate[0] - p.x;
			double dy = coordinate[1] - p.y;
			double dz = coordinate[2] - p.z;

			return dx*dx + dy*dy + dz*dz;
		}

		/**
		 * Normalize a point in homogenous coordinate so that it's f-norm is 1
		 */
		public void normalizeH() {
			double n = 0;
			n += coordinate[0]*coordinate[0];
			n += coordinate[1]*coordinate[1];
			n += coordinate[2]*coordinate[2];
			n += coordinate[3]*coordinate[3];

			n = Math.sqrt(n);

			coordinate[0] /= n;
			coordinate[1] /= n;
			coordinate[2] /= n;
			coordinate[3] /= n;
		}

		public double distance( Point3D_F64 p ) {
			return Math.sqrt(distance(p));
		}

		public double distance( Point p ) {
			double dx = coordinate[0] - p.coordinate[0];
			double dy = coordinate[1] - p.coordinate[1];
			double dz = coordinate[2] - p.coordinate[2];

			return Math.sqrt(dx*dx + dy*dy + dz*dz);
		}
	}

	@Override
	public boolean isHomogenous() {
		return homogenous;
	}

	public int getObservationCount() {
		int total = 0;
		for (int i = 0; i < points.size; i++) {
			total += points.data[i].views.size;
		}
		return total;
	}
}
