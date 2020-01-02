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

import java.util.List;

import static boofcv.alg.cloud.PointCloudUtils.convert;

/**
 * Interface for reading a point cloud
 *
 * @author Peter Abeles
 */
public interface PointCloudReader {
	/**
	 * Number of points
	 */
	int size();

	/**
	 * Copies the point
	 */
	void get(int index , Point3D_F32 point );

	/**
	 * Copies the point
	 */
	void get(int index , Point3D_F64 point );

	/**
	 * RGB value of point
	 */
	int getRGB( int index );

	static PointCloudReader wrap3FRGB( float[] cloud, float[] rgb, int offset , int length ) {
		return new PointCloudReader() {
			@Override
			public int size() {return length;}

			@Override
			public void get(int index, Point3D_F32 point) {
				int i = offset + index*3;
				point.set(cloud[i],cloud[i+1],cloud[i+2]);
			}

			@Override
			public void get(int index, Point3D_F64 point) {
				int i = offset + index*3;
				point.set(cloud[i],cloud[i+1],cloud[i+2]);
			}

			@Override
			public int getRGB(int index) {
				int i = offset + index*3;
				int r = (int)(rgb[i  ]*255);
				int g = (int)(rgb[i+1]*255);
				int b = (int)(rgb[i+2]*255);
				return (r << 16) | (g << 8) | b;
			}
		};
	}

	static PointCloudReader wrapF32( List<Point3D_F32> cloud ) {
		return new PointCloudReader() {
			@Override
			public int size() {return cloud.size();}

			@Override
			public void get(int index, Point3D_F32 point) {point.set(cloud.get(index));}

			@Override
			public void get(int index, Point3D_F64 point) {convert(cloud.get(index),point);}

			@Override
			public int getRGB(int index) {return 0;}
		};
	}

	static PointCloudReader wrapF64( List<Point3D_F64> cloud ) {
		return new PointCloudReader() {
			@Override
			public int size() {return cloud.size();}

			@Override
			public void get(int index, Point3D_F32 point) {convert(cloud.get(index),point);}

			@Override
			public void get(int index, Point3D_F64 point) {point.set(cloud.get(index));}

			@Override
			public int getRGB(int index) {return 0;}
		};
	}

	static PointCloudReader wrapF32RGB( List<Point3dRgbI_F32> cloud ) {
		return new PointCloudReader() {
			@Override
			public int size() {return cloud.size();}

			@Override
			public void get(int index, Point3D_F32 point) {point.set(cloud.get(index));}

			@Override
			public void get(int index, Point3D_F64 point) {convert(cloud.get(index),point);}

			@Override
			public int getRGB(int index) {return cloud.get(index).rgb;}
		};
	}

	static PointCloudReader wrapF64RGB( List<Point3dRgbI_F64> cloud ) {
		return new PointCloudReader() {
			@Override
			public int size() {return cloud.size();}

			@Override
			public void get(int index, Point3D_F32 point) {convert(cloud.get(index),point);}

			@Override
			public void get(int index, Point3D_F64 point) {point.set(cloud.get(index));}

			@Override
			public int getRGB(int index) {return cloud.get(index).rgb;}
		};
	}
}