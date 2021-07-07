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

package boofcv.examples.pointclouds;

import boofcv.alg.cloud.PointCloudReader;
import boofcv.io.points.PointCloudIO;
import boofcv.struct.PackedArray;
import boofcv.struct.packed.PackedBigArrayPoint3D_F64;
import georegression.struct.point.Point3D_F64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Example showing how to load and save a point cloud.
 *
 * @author Peter Abeles
 */
public class ExampleLoadAndSavePointCloud {
	public static void main( String[] args ) throws IOException {
		File file = File.createTempFile("boofcv", "txt");

		// Creating a point cloud and storing it in a more "exotic" format used when dealing with a very large
		// number of points.
		PackedArray<Point3D_F64> original = new PackedBigArrayPoint3D_F64();
		for (int i = 0; i < 100; i++) {
			// create arbitrary points
			original.append(new Point3D_F64(i, i + 1, -i));
		}

		System.out.println("original.size=" + original.size());

		// Save the colorless point cloud
		PointCloudIO.save3D(PointCloudIO.Format.PLY,
				PointCloudReader.wrap(
						// passed in (x,y,z) and rgb info
						( index, point ) -> point.setTo(original.getTemp(index)),
						// specifies the number of points
						original.size()),
				/* saveRGB */false, new FileOutputStream(file));

		// create another arbitrary format to store the loaded results
		List<Point3D_F64> found = new ArrayList<>();

		PointCloudIO.load(PointCloudIO.Format.PLY, new FileInputStream(file),
				( x, y, z, rgb ) -> found.add(new Point3D_F64(x, y, z)));
		// There's a non functional version of load() which will provide the writer with information about the
		// clouds size and format.

		System.out.println("found.size=" + found.size());

		// clean up
		file.delete();
	}
}
