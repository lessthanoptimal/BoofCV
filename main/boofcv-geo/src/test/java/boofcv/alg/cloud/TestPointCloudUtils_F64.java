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

import boofcv.struct.Point3dRgbI_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPointCloudUtils_F64 extends BoofStandardJUnit {

	@Test void filter() {
		DogArray<Point3dRgbI_F64> input = new DogArray<>(Point3dRgbI_F64::new);
		for (int i = 0; i < 20; i++) {
			input.grow().setTo(i,i,i);
		}

		DogArray<Point3dRgbI_F64> output = PointCloudUtils_F64.filter(
				(idx, p)->p.setTo(input.get(idx)), (idx)->input.get(idx).rgb, input.size, (idx)->idx<9,null);

		assertEquals(9, output.size);
	}

	@Test void autoScale() {
		Point3D_F64 mean = new Point3D_F64(1, 2, 3);
		Point3D_F64 stdev = new Point3D_F64(2, 0.5, 0.1);

		List<Point3D_F64> list = new ArrayList<>();

		for (int i = 0; i < 20000; i++) {
			Point3D_F64 p = new Point3D_F64();
			p.x = (double)rand.nextGaussian()*stdev.x + mean.x;
			p.y = (double)rand.nextGaussian()*stdev.y + mean.y;
			p.z = (double)rand.nextGaussian()*stdev.z + mean.z;
			list.add(p);
		}

		double scale = PointCloudUtils_F64.autoScale(list, 10);

		Point3D_F64 foundMean = new Point3D_F64();
		Point3D_F64 foundStdev = new Point3D_F64();
		PointCloudUtils_F64.statistics(list, foundMean, foundStdev);

		foundMean.scale(1.0/scale);

		double tol = Math.pow(UtilEjml.TEST_F64, 1.0/4.5);
		assertEquals(0, foundMean.distance(mean), tol);

		double maxStdev = Math.max(Math.max(foundStdev.x, foundStdev.y), foundStdev.z);

		assertEquals(10, maxStdev, tol);
	}

	@Test void statistics() {
		Point3D_F64 mean = new Point3D_F64(1, 2, 3);
		Point3D_F64 stdev = new Point3D_F64(2, 0.5, 0.1);

		List<Point3D_F64> list = new ArrayList<>();

		for (int i = 0; i < 20000; i++) {
			Point3D_F64 p = new Point3D_F64();
			p.x = (double)rand.nextGaussian()*stdev.x + mean.x;
			p.y = (double)rand.nextGaussian()*stdev.y + mean.y;
			p.z = (double)rand.nextGaussian()*stdev.z + mean.z;
			list.add(p);
		}

		Point3D_F64 foundMean = new Point3D_F64();
		Point3D_F64 foundStdev = new Point3D_F64();

		PointCloudUtils_F64.statistics(list, foundMean, foundStdev);

		double tol = Math.pow(UtilEjml.TEST_F64, 1.0/4.5);
		assertEquals(0, foundMean.distance(mean), tol);
		assertEquals(0, foundStdev.distance(stdev), tol);
	}

	@Test void prune() {
		List<Point3D_F64> list = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			list.add(new Point3D_F64(i*0.1, 0, 0));
		}

		PointCloudUtils_F64.prune(list, 3, 0.31);
		assertEquals(100, list.size());

		// end points should be pruned
		PointCloudUtils_F64.prune(list, 3, 0.28);
		assertEquals(98, list.size());
	}

	@Test void prune_color() {
		List<Point3D_F64> list = new ArrayList<>();
		DogArray_I32 rgb = new DogArray_I32();
		for (int i = 0; i < 100; i++) {
			list.add(new Point3D_F64(i*0.1, 0, 0));
			rgb.add(i);
		}

		PointCloudUtils_F64.prune(list, rgb, 3, 0.31);
		assertEquals(100, list.size());
		assertEquals(100, rgb.size());

		// end points should be pruned
		PointCloudUtils_F64.prune(list, rgb, 3, 0.28);
		assertEquals(98, list.size());
		assertEquals(98, rgb.size());
		assertEquals(1, rgb.get(0));
		assertEquals(97, rgb.get(96));
	}
}
