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

import boofcv.alg.cloud.PointCloudReader;
import boofcv.alg.cloud.PointCloudWriter;
import boofcv.io.points.impl.PlyCodec;
import boofcv.struct.Point3dRgbI_F64;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.FastQueue;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteOrder;

/**
 * Code for reading different point cloud formats
 *
 * @author Peter Abeles
 */
public class PointCloudIO {

	public static void save3D(Format format, PointCloudReader cloud , boolean saveRGB, OutputStream outputStream ) throws IOException {
		switch( format ) {
			case PLY_ASCII:
				PlyCodec.saveAscii(cloud, saveRGB, new OutputStreamWriter(outputStream));
				break;
			case PLY_BINARY:
				PlyCodec.saveBinary(cloud, ByteOrder.BIG_ENDIAN, saveRGB,true, outputStream);
				break;

			default:
				throw new IllegalArgumentException("Unknown format "+format);
		}
	}


	public static FastQueue<Point3D_F32>
	load3D32F( Format format , InputStream input , @Nullable FastQueue<Point3D_F32> storage  ) throws IOException {
		if( storage == null )
			storage = new FastQueue<>(Point3D_F32.class,true);
		PointCloudWriter output = PointCloudWriter.wrapF32(storage);
		load(format,input,output);
		return storage;
	}

	public static FastQueue<Point3D_F64>
	load3D64F( Format format , InputStream input , @Nullable FastQueue<Point3D_F64> storage  ) throws IOException {
		if( storage == null )
			storage = new FastQueue<>(Point3D_F64.class,true);
		PointCloudWriter output = PointCloudWriter.wrapF64(storage);
		load(format,input,output);
		return storage;
	}

	public static FastQueue<Point3dRgbI_F64>
	load3DRgb64F(Format format , InputStream input , @Nullable FastQueue<Point3dRgbI_F64> storage  ) throws IOException {
		if( storage == null )
			storage = new FastQueue<>(Point3dRgbI_F64.class,true);
		PointCloudWriter output = PointCloudWriter.wrapF64RGB(storage);
		load(format,input,output);
		return storage;
	}

	public static void
	load(Format format , InputStream input , PointCloudWriter output ) throws IOException {
		switch( format ) {
			case PLY_ASCII:
			case PLY_BINARY:
				PlyCodec.read(input,output);
				break;
			default:
				throw new IllegalArgumentException("Unknown format "+format);
		}
	}

	public enum Format {
		/**
		 * https://en.wikipedia.org/wiki/PLY_(file_format)
		 */
		PLY_ASCII,PLY_BINARY
	}
}
