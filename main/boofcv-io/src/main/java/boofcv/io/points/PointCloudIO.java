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

package boofcv.io.points;

import boofcv.alg.cloud.AccessColorIndex;
import boofcv.alg.cloud.AccessPointIndex;
import boofcv.alg.cloud.PointCloudReader;
import boofcv.alg.cloud.PointCloudWriter;
import boofcv.io.points.impl.PlyCodec;
import boofcv.struct.Point3dRgbI_F64;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

/**
 * Code for reading different point cloud formats
 *
 * @author Peter Abeles
 */
public class PointCloudIO {
	/**
	 * Saves point cloud to disk using a high level API. For more control over the format use the CODEC directly.
	 *
	 * @see PlyCodec
	 */
	public static void save3D( Format format, PointCloudReader cloud, boolean saveRGB, OutputStream outputStream )
			throws IOException {
		switch (format) {
			case PLY -> PlyCodec.saveBinary(cloud, ByteOrder.BIG_ENDIAN, saveRGB, false, outputStream);
		}
	}

	/**
	 * Saves point cloud using access API.
	 *
	 * @see AccessPointIndex
	 * @see AccessColorIndex
	 * @see PlyCodec
	 */
	public static void save3D( Format format,
							   AccessPointIndex<Point3D_F64> accessPoint,
							   AccessColorIndex accessColor,
							   int size , boolean saveRGB, OutputStream outputStream )
			throws IOException {

		PointCloudReader reader = new PointCloudReader() {
			final Point3D_F64 tmp = new Point3D_F64();
			@Override public void get( int index, Point3D_F32 point ) {
				accessPoint.getPoint(index, tmp);
				point.setTo((float)tmp.x, (float)tmp.y, (float)tmp.z);
			}

			@Override public int size() {return size;}
			@Override public void get( int index, Point3D_F64 point ) {accessPoint.getPoint(index, point);}
			@Override public int getRGB( int index ) {return accessColor.getRGB(index);}
		};

		save3D(format, reader, saveRGB, outputStream);
	}

	public static DogArray<Point3D_F32>
	load3D32F( Format format, InputStream input, @Nullable DogArray<Point3D_F32> storage ) throws IOException {
		if (storage == null)
			storage = new DogArray<>(Point3D_F32::new);
		PointCloudWriter output = PointCloudWriter.wrapF32(storage);
		load(format, input, output);
		return storage;
	}

	public static DogArray<Point3D_F64>
	load3D64F( Format format, InputStream input, @Nullable DogArray<Point3D_F64> storage ) throws IOException {
		if (storage == null)
			storage = new DogArray<>(Point3D_F64::new);
		PointCloudWriter output = PointCloudWriter.wrapF64(storage);
		load(format, input, output);
		return storage;
	}

	public static DogArray<Point3dRgbI_F64>
	load3DRgb64F( Format format, InputStream input, @Nullable DogArray<Point3dRgbI_F64> storage ) throws IOException {
		if (storage == null)
			storage = new DogArray<>(Point3dRgbI_F64::new);
		PointCloudWriter output = PointCloudWriter.wrapF64RGB(storage);
		load(format, input, output);
		return storage;
	}

	/**
	 * Reads a point cloud from the input stream in the specified format and writes it to the output.
	 *
	 * @param format Storage format
	 * @param input Input stream
	 * @param output Output cloud writer
	 */
	public static void load( Format format, InputStream input, PointCloudWriter output ) throws IOException {
		switch (format) {
			case PLY -> PlyCodec.read(input, output);
		}
	}

	/**
	 * The same as {@link #load(Format, InputStream, PointCloudWriter)}, but with a simplified writer that
	 * removes the initialization function. Result is more concise code with less flexibility
	 */
	public static void load( Format format, InputStream input, FunctionalWriter output ) throws IOException {
		PointCloudWriter pcw = new PointCloudWriter() {
			@Override public void initialize( int size, boolean hasColor ) {}

			@Override public void add( double x, double y, double z, int rgb ) {
				output.add(x, y, z, rgb);
			}
		};
		load(format, input, pcw);
	}

	/**
	 * A writer without the initialization step. Used to simplify the code
	 */
	@FunctionalInterface
	public interface FunctionalWriter {
		void add( double x, double y, double z, int rgb );
	}

	public enum Format {
		/**
		 * https://en.wikipedia.org/wiki/PLY_(file_format)
		 *
		 * @see PlyCodec
		 */
		PLY
	}
}
