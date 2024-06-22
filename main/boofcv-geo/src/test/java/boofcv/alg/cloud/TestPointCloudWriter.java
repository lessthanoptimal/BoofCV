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

import boofcv.alg.cloud.PointCloudWriter.CloudArraysF32;
import boofcv.struct.Point3dRgbI_F32;
import boofcv.struct.Point3dRgbI_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestPointCloudWriter extends BoofStandardJUnit {

	static abstract class PcwTests {
		boolean supportsColor = true;

		public abstract PointCloudWriter create();

		public abstract int size( PointCloudWriter data );

		public abstract Point3D_F64 getPoint( PointCloudWriter data, int i );

		public abstract int getColor( PointCloudWriter data, int i );

		@Test
		void simpleXyzColor() {
			PointCloudWriter alg = create();

			assertEquals(0, size(alg));
			alg.startPoint();
			alg.location(0, 1, 2);
			alg.color(345);
			alg.stopPoint();
			assertEquals(1, size(alg));
			alg.startPoint();
			alg.location(2, 1, 3);
			alg.color(3434);
			alg.stopPoint();
			assertEquals(2, size(alg));

			if (supportsColor) {
				assertEquals(345, getColor(alg, 0));
				assertEquals(3434, getColor(alg, 1));
			}

			Point3D_F64 found = getPoint(alg, 1);
			assertEquals(2, found.x, UtilEjml.TEST_F64);
			assertEquals(1, found.y, UtilEjml.TEST_F64);
			assertEquals(3, found.z, UtilEjml.TEST_F64);
		}
	}

	@Nested
	public class CheckCloudArraysF32 extends PcwTests {

		@Override
		public PointCloudWriter create() {
			return new PointCloudWriter.CloudArraysF32();
		}

		@Override
		public int size( PointCloudWriter data ) {
			return ((CloudArraysF32)data).cloudXyz.size/3;
		}

		@Override
		public Point3D_F64 getPoint( PointCloudWriter data, int i ) {
			var list = ((CloudArraysF32)data).cloudXyz;
			return new Point3D_F64(list.get(i*3), list.get(i*3 + 1), list.get(i*3 + 2));
		}

		@Override
		public int getColor( PointCloudWriter data, int i ) {
			var list = ((CloudArraysF32)data).cloudRgb;
			return list.get(i);
		}
	}

	@Nested
	public class CheckWrapF32 extends PcwTests {

		DogArray<Point3D_F32> cloud;

		@Override
		public PointCloudWriter create() {
			supportsColor = false;
			cloud = new DogArray<>(Point3D_F32::new);
			return PointCloudWriter.wrapF32(cloud);
		}

		@Override
		public int size( PointCloudWriter data ) {
			return cloud.size;
		}

		@Override
		public Point3D_F64 getPoint( PointCloudWriter data, int i ) {
			Point3D_F32 c = cloud.get(i);
			return new Point3D_F64(c.x, c.y, c.z);
		}

		@Override
		public int getColor( PointCloudWriter data, int i ) {
			throw new RuntimeException("Not supported");
		}
	}

	@Nested
	public class CheckWrapF64 extends PcwTests {

		DogArray<Point3D_F64> cloud;

		@Override
		public PointCloudWriter create() {
			supportsColor = false;
			cloud = new DogArray<>(Point3D_F64::new);
			return PointCloudWriter.wrapF64(cloud);
		}

		@Override
		public int size( PointCloudWriter data ) {
			return cloud.size;
		}

		@Override
		public Point3D_F64 getPoint( PointCloudWriter data, int i ) {
			return cloud.get(i);
		}

		@Override
		public int getColor( PointCloudWriter data, int i ) {
			throw new RuntimeException("Not supported");
		}
	}

	@Nested
	public class CheckWrapF32RGB extends PcwTests {

		DogArray<Point3dRgbI_F32> cloud;

		@Override
		public PointCloudWriter create() {
			cloud = new DogArray<>(Point3dRgbI_F32::new);
			return PointCloudWriter.wrapF32RGB(cloud);
		}

		@Override
		public int size( PointCloudWriter data ) {
			return cloud.size;
		}

		@Override
		public Point3D_F64 getPoint( PointCloudWriter data, int i ) {
			Point3D_F32 c = cloud.get(i);
			return new Point3D_F64(c.x, c.y, c.z);
		}

		@Override
		public int getColor( PointCloudWriter data, int i ) {
			return cloud.get(i).rgb;
		}
	}

	@Nested
	public class CheckWrap642RGB extends PcwTests {

		DogArray<Point3dRgbI_F64> cloud;

		@Override
		public PointCloudWriter create() {
			cloud = new DogArray<>(Point3dRgbI_F64::new);
			return PointCloudWriter.wrapF64RGB(cloud);
		}

		@Override
		public int size( PointCloudWriter data ) {
			return cloud.size;
		}

		@Override
		public Point3D_F64 getPoint( PointCloudWriter data, int i ) {
			return cloud.get(i);
		}

		@Override
		public int getColor( PointCloudWriter data, int i ) {
			return cloud.get(i).rgb;
		}
	}
}
