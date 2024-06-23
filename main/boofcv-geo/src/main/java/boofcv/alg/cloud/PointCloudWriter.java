/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.cloud;

import boofcv.struct.Point3dRgbI_F32;
import boofcv.struct.Point3dRgbI_F64;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;

/**
 * Interface for reading a point cloud
 *
 * @author Peter Abeles
 */
public interface PointCloudWriter {
	/**
	 * Initializes and passes in configurations.
	 *
	 * @param size Estimated size of the point cloud. Passes in a value &le; 0 if it's unknown
	 * @param hasColor true if the cloud has color information
	 */
	void initialize( int size, boolean hasColor );

	/** A new point is being added with new attributes to follow */
	void startPoint();

	/** It's done specifying attributes for the point */
	void stopPoint();

	/**
	 * Set the 3D location of the point
	 */
	void location( double x, double y, double z );

	/**
	 * Sets the points color
	 */
	void color( int rgb );

	class CloudArraysF32 implements PointCloudWriter {
		// Storage for point cloud
		public DogArray_F32 cloudXyz = new DogArray_F32();
		public DogArray_I32 cloudRgb = new DogArray_I32();

		@Override public void initialize( int size, boolean hasColor ) {
			cloudRgb.reset();
			cloudXyz.reset();
			cloudRgb.reserve(size);
			cloudXyz.reserve(size*3);
		}

		@Override public void startPoint() {}

		@Override public void stopPoint() {}

		@Override public void location( double x, double y, double z ) {
			cloudXyz.add((float)x);
			cloudXyz.add((float)y);
			cloudXyz.add((float)z);
		}

		@Override public void color( int rgb ) {
			cloudRgb.add(rgb);
		}

		public void getPoint( int index, Point3D_F64 p ) {
			int i = index*3;
			p.x = cloudXyz.data[i];
			p.y = cloudXyz.data[i + 1];
			p.z = cloudXyz.data[i + 2];
		}
	}

	static PointCloudWriter wrapF32( DogArray<Point3D_F32> cloud ) {
		return new PointCloudWriter() {
			@Override public void initialize( int size, boolean hasColor ) {
				cloud.reserve(size);
				cloud.reset();
			}

			@Override public void startPoint() {}

			@Override public void stopPoint() {}

			@Override public void location( double x, double y, double z ) {
				cloud.grow().setTo((float)x, (float)y, (float)z);
			}

			@Override public void color( int rgb ) {}
		};
	}

	static PointCloudWriter wrapF64( DogArray<Point3D_F64> cloud ) {
		return new PointCloudWriter() {
			@Override public void initialize( int size, boolean hasColor ) {
				cloud.reserve(size);
				cloud.reset();
			}

			@Override public void startPoint() {}

			@Override public void stopPoint() {}

			@Override public void color( int rgb ) {}

			@Override public void location( double x, double y, double z ) {
				cloud.grow().setTo(x, y, z);
			}
		};
	}

	static PointCloudWriter wrapF64( DogArray<Point3D_F64> cloud, DogArray_I32 colors ) {
		return new PointCloudWriter() {
			@Override public void initialize( int size, boolean hasColor ) {
				cloud.reserve(size);
				cloud.reset();
				colors.reserve(size);
				colors.reset();
			}

			@Override public void startPoint() {}

			@Override public void stopPoint() {}

			@Override public void color( int rgb ) {colors.add(rgb);}

			@Override public void location( double x, double y, double z ) {cloud.grow().setTo(x, y, z);}
		};
	}

	static PointCloudWriter wrapF32RGB( DogArray<Point3dRgbI_F32> cloud ) {
		return new PointCloudWriter() {
			@Override public void initialize( int size, boolean hasColor ) {
				cloud.reserve(size);
				cloud.reset();
			}

			@Override public void startPoint() {
				cloud.grow();
			}

			@Override public void stopPoint() {}

			@Override public void location( double x, double y, double z ) {
				cloud.getTail().setTo((float)x, (float)y, (float)z);
			}

			@Override public void color( int rgb ) {
				cloud.getTail().rgb = rgb;
			}
		};
	}

	static PointCloudWriter wrapF64RGB( DogArray<Point3dRgbI_F64> cloud ) {
		return new PointCloudWriter() {
			@Override public void initialize( int size, boolean hasColor ) {
				cloud.reset();
			}

			@Override public void startPoint() {
				cloud.grow();
			}

			@Override public void stopPoint() {}

			@Override public void location( double x, double y, double z ) {
				cloud.getTail().setTo(x, y, z);
			}

			@Override public void color( int rgb ) {
				cloud.getTail().rgb = rgb;
			}
		};
	}
}
