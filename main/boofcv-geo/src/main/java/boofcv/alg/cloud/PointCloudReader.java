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

import java.util.List;

import static georegression.struct.ConvertFloatType.convert;

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

	/** True if each point has a color */
	boolean colors();

	/**
	 * Copies the point
	 */
	void get( int index, Point3D_F32 point );

	/**
	 * Copies the point
	 */
	void get( int index, Point3D_F64 point );

	/**
	 * RGB value of point
	 */
	int getRGB( int index );

	static PointCloudReader wrap3FRGB( float[] cloud, float[] rgb, int offset, int length ) {
		return new PointCloudReader() {
			@Override
			public int size() {return length;}

			@Override public boolean colors() {return true;}

			@Override
			public void get( int index, Point3D_F32 point ) {
				int i = offset + index*3;
				point.setTo(cloud[i], cloud[i + 1], cloud[i + 2]);
			}

			@Override
			public void get( int index, Point3D_F64 point ) {
				int i = offset + index*3;
				point.setTo(cloud[i], cloud[i + 1], cloud[i + 2]);
			}

			@Override
			public int getRGB( int index ) {
				int i = offset + index*3;
				int r = (int)(rgb[i]*255);
				int g = (int)(rgb[i + 1]*255);
				int b = (int)(rgb[i + 2]*255);
				return (r << 16) | (g << 8) | b;
			}
		};
	}

	static PointCloudReader wrapF32( List<Point3D_F32> cloud ) {
		return new PointCloudReader() {
			@Override public int size() {return cloud.size();}

			@Override public boolean colors() {return false;}

			@Override public void get( int index, Point3D_F32 point ) {point.setTo(cloud.get(index));}

			@Override public void get( int index, Point3D_F64 point ) {convert(cloud.get(index), point);}

			@Override public int getRGB( int index ) {return 0;}
		};
	}

	static PointCloudReader wrapF64( List<Point3D_F64> cloud ) {
		return new PointCloudReader() {
			@Override public int size() {return cloud.size();}

			@Override public boolean colors() {return false;}

			@Override public void get( int index, Point3D_F32 point ) {convert(cloud.get(index), point);}

			@Override public void get( int index, Point3D_F64 point ) {point.setTo(cloud.get(index));}

			@Override public int getRGB( int index ) {return 0;}
		};
	}

	static PointCloudReader wrapF32RGB( List<Point3dRgbI_F32> cloud ) {
		return new PointCloudReader() {
			@Override public int size() {return cloud.size();}

			@Override public boolean colors() {return true;}

			@Override public void get( int index, Point3D_F32 point ) {point.setTo(cloud.get(index));}

			@Override public void get( int index, Point3D_F64 point ) {convert(cloud.get(index), point);}

			@Override public int getRGB( int index ) {return cloud.get(index).rgb;}
		};
	}

	static PointCloudReader wrapF64RGB( List<Point3dRgbI_F64> cloud ) {
		return new PointCloudReader() {
			@Override public int size() {return cloud.size();}

			@Override public boolean colors() {return true;}

			@Override public void get( int index, Point3D_F32 point ) {convert(cloud.get(index), point);}

			@Override public void get( int index, Point3D_F64 point ) {point.setTo(cloud.get(index));}

			@Override public int getRGB( int index ) {return cloud.get(index).rgb;}
		};
	}

	static PointCloudReader wrapF32( List<Point3D_F32> cloud, int[] rgb ) {
		return new PointCloudReader() {
			@Override public int size() {return cloud.size();}

			@Override public boolean colors() {return true;}

			@Override public void get( int index, Point3D_F32 point ) {point.setTo(cloud.get(index));}

			@Override public void get( int index, Point3D_F64 point ) {convert(cloud.get(index), point);}

			@Override public int getRGB( int index ) {return rgb[index];}
		};
	}

	static PointCloudReader wrapF64( List<Point3D_F64> cloud, int[] rgb ) {
		return new PointCloudReader() {
			@Override public int size() {return cloud.size();}

			@Override public boolean colors() {return true;}

			@Override public void get( int index, Point3D_F32 point ) {convert(cloud.get(index), point);}

			@Override public void get( int index, Point3D_F64 point ) {point.setTo(cloud.get(index));}

			@Override public int getRGB( int index ) {return rgb[index];}
		};
	}

	static PointCloudReader wrap( Generic op, int size ) {
		return new PointCloudReader() {
			Point3dRgbI_F64 p = new Point3dRgbI_F64();

			@Override public int size() {return size;}

			@Override public boolean colors() {return true;}

			@Override public void get( int index, Point3D_F32 point ) {
				op.get(index, p);
				point.x = (float)p.x;
				point.y = (float)p.y;
				point.z = (float)p.z;
			}

			@Override public void get( int index, Point3D_F64 point ) {
				op.get(index, p);
				point.setTo(p);
			}

			@Override public int getRGB( int index ) {
				op.get(index, p);
				return p.rgb;
			}
		};
	}

	interface Generic {
		void get( int index, Point3dRgbI_F64 point );
	}
}
