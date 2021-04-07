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
	 * Resets and initializes the writer and tells it approximately how many points will be written so it can
	 * preallocate memory
	 *
	 * @param estimatedSize Approximate number of points which will be written.
	 */
	void init( int estimatedSize );

	/**
	 * Adds a 3D point
	 */
	void add( double x, double y, double z );

	/**
	 * Adds a 3D point with color information
	 */
	void add( double x, double y, double z, int rgb );

	class CloudArraysF32 implements PointCloudWriter {
		// Storage for point cloud
		public DogArray_F32 cloudXyz = new DogArray_F32();
		public DogArray_I32 cloudRgb = new DogArray_I32();

		@Override
		public void init( int estimatedSize ) {
			cloudRgb.reset();
			cloudXyz.reset();
			cloudRgb.reserve(estimatedSize);
			cloudXyz.reserve(estimatedSize*3);
		}

		@Override
		public void add( double x, double y, double z ) {
			cloudXyz.add((float)x);
			cloudXyz.add((float)y);
			cloudXyz.add((float)z);
		}

		@Override
		public void add( double x, double y, double z, int rgb ) {
			cloudXyz.add((float)x);
			cloudXyz.add((float)y);
			cloudXyz.add((float)z);
			cloudRgb.add(rgb);
		}
	}

	static PointCloudWriter wrapF32( DogArray<Point3D_F32> cloud ) {
		return new PointCloudWriter() {
			@Override
			public void init( int estimatedSize ) {
				cloud.reserve(estimatedSize);
				cloud.reset();
			}

			@Override
			public void add( double x, double y, double z ) {
				cloud.grow().setTo((float)x, (float)y, (float)z);
			}

			@Override
			public void add( double x, double y, double z, int rgb ) {
				cloud.grow().setTo((float)x, (float)y, (float)z);
			}
		};
	}

	static PointCloudWriter wrapF64( DogArray<Point3D_F64> cloud ) {
		return new PointCloudWriter() {
			@Override
			public void init( int estimatedSize ) {
				cloud.reserve(estimatedSize);
				cloud.reset();
			}

			@Override
			public void add( double x, double y, double z ) {
				cloud.grow().setTo(x, y, z);
			}

			@Override
			public void add( double x, double y, double z, int rgb ) {
				cloud.grow().setTo(x, y, z);
			}
		};
	}

	static PointCloudWriter wrapF32RGB( DogArray<Point3dRgbI_F32> cloud ) {
		return new PointCloudWriter() {
			@Override
			public void init( int estimatedSize ) {
				cloud.reserve(estimatedSize);
				cloud.reset();
			}

			@Override
			public void add( double x, double y, double z ) {
				cloud.grow().setTo((float)x, (float)y, (float)z);
			}

			@Override
			public void add( double x, double y, double z, int rgb ) {
				cloud.grow().setTo((float)x, (float)y, (float)z, rgb);
			}
		};
	}

	static PointCloudWriter wrapF64RGB( DogArray<Point3dRgbI_F64> cloud ) {
		return new PointCloudWriter() {
			@Override
			public void init( int estimatedSize ) {
				cloud.reserve(estimatedSize);
				cloud.reset();
			}

			@Override
			public void add( double x, double y, double z ) {
				cloud.grow().setTo(x, y, z);
			}

			@Override
			public void add( double x, double y, double z, int rgb ) {
				cloud.grow().setTo(x, y, z, rgb);
			}
		};
	}
}
