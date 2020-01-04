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

package boofcv.io.points;

import boofcv.struct.Point3dRgbI_F32;
import boofcv.struct.Point3dRgbI_F64;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * Interface for reading a point cloud
 *
 * @author Peter Abeles
 */
public interface PointCloudWriter {
	void init( int estimatedSize );

	void add( double x , double y , double z );

	void add( double x , double y , double z , int rgb );

	static PointCloudWriter wrapF32(FastQueue<Point3D_F32> cloud) {
		return new PointCloudWriter() {
			@Override
			public void init(int estimatedSize) {
				cloud.growArray(estimatedSize);
				cloud.reset();
			}

			@Override
			public void add(double x, double y, double z) {
				cloud.grow().set((float)x, (float)y, (float)z);
			}

			@Override
			public void add(double x, double y, double z, int rgb) {
				cloud.grow().set((float)x, (float)y, (float)z);
			}
		};
	}

	static PointCloudWriter wrapF64(FastQueue<Point3D_F64> cloud) {
		return new PointCloudWriter() {
			@Override
			public void init(int estimatedSize) {
				cloud.growArray(estimatedSize);
				cloud.reset();
			}

			@Override
			public void add(double x, double y, double z) {
				cloud.grow().set(x, y, z);
			}

			@Override
			public void add(double x, double y, double z, int rgb) {
				cloud.grow().set(x, y, z);
			}
		};
	}

	static PointCloudWriter wrapF32RGB(FastQueue<Point3dRgbI_F32> cloud) {
		return new PointCloudWriter() {
			@Override
			public void init(int estimatedSize) {
				cloud.growArray(estimatedSize);
				cloud.reset();
			}

			@Override
			public void add(double x, double y, double z) {
				cloud.grow().set((float)x, (float)y, (float)z);
			}

			@Override
			public void add(double x, double y, double z, int rgb) {
				cloud.grow().set((float)x, (float)y, (float)z,rgb);
			}
		};
	}

	static PointCloudWriter wrapF64RGB(FastQueue<Point3dRgbI_F64> cloud) {
		return new PointCloudWriter() {
			@Override
			public void init(int estimatedSize) {
				cloud.growArray(estimatedSize);
				cloud.reset();
			}

			@Override
			public void add(double x, double y, double z) {
				cloud.grow().set(x, y, z);
			}

			@Override
			public void add(double x, double y, double z, int rgb) {
				cloud.grow().set(x, y, z,rgb);
			}
		};
	}
}
