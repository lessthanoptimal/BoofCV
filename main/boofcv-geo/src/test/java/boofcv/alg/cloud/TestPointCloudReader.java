/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestPointCloudReader extends BoofStandardJUnit {
	static abstract class ReaderTests {
		public abstract PointCloudReader createReader(List<Point3dRgbI_F64> points );

		@Test
		void checkXYZ() {
			var points = new ArrayList<Point3dRgbI_F64>();
			points.add( new Point3dRgbI_F64(1,2,3,3405));
			points.add( new Point3dRgbI_F64(6,1,7,938));

			PointCloudReader reader = createReader(points);

			assertEquals(2, reader.size());
			checkXYZ(reader,0,points.get(0));
			checkXYZ(reader,1,points.get(1));
		}

		@Test
		void checkRgb() {
			var points = new ArrayList<Point3dRgbI_F64>();
			points.add( new Point3dRgbI_F64(1,2,3,3405));
			points.add( new Point3dRgbI_F64(6,1,7,938));

			PointCloudReader reader = createReader(points);

			assertEquals(2, reader.size());
			assertEquals(3405,reader.getRGB(0));
			assertEquals(938,reader.getRGB(1));
		}

		private void checkXYZ( PointCloudReader reader, int index, Point3dRgbI_F64 expected ) {
			var p64 = new Point3D_F64();
			var p32 = new Point3D_F32();

			reader.get(index,p64);

			// check error with 32-bit since it might be stored in that format interally
			assertEquals(expected.x, p64.x, UtilEjml.TEST_F32);
			assertEquals(expected.y, p64.y, UtilEjml.TEST_F32);
			assertEquals(expected.z, p64.z, UtilEjml.TEST_F32);

			reader.get(index,p32);
			assertEquals(expected.x, p32.x, UtilEjml.TEST_F32);
			assertEquals(expected.y, p32.y, UtilEjml.TEST_F32);
			assertEquals(expected.z, p32.z, UtilEjml.TEST_F32);
		}
	}

	@Nested
	public class CheckWrap3FRGB extends ReaderTests {
		@Override
		public PointCloudReader createReader(List<Point3dRgbI_F64> points) {
			int N = points.size();
			float[] cloud = new float[3*N];
			float[] rgb = new float[3*N];

			for (int i = 0; i < N; i++) {
				var p = points.get(i);
				cloud[i*3  ] = (float)p.x;
				cloud[i*3+1] = (float)p.y;
				cloud[i*3+2] = (float)p.z;
				rgb[i*3  ] = ((float)((p.rgb>>16)&0xFF))/255.0f;
				rgb[i*3+1] = ((float)((p.rgb>>8 )&0xFF))/255.0f;
				rgb[i*3+2] = ((float)((p.rgb    )&0xFF))/255.0f;
			}

			return PointCloudReader.wrap3FRGB(cloud,rgb,0,N);
		}
	}

	@Nested
	public class CheckWrapF32 extends ReaderTests {
		@Override
		public PointCloudReader createReader(List<Point3dRgbI_F64> points) {
			var cloud = new ArrayList<Point3D_F32>();
			for (Point3dRgbI_F64 p : points) {
				var c = new Point3D_F32();
				c.setTo((float) p.x, (float) p.y, (float) p.z);
				cloud.add(c);
			}

			return PointCloudReader.wrapF32(cloud);
		}

		@Override void checkRgb() {}
	}

	@Nested
	public class CheckWrapF64 extends ReaderTests {
		@Override
		public PointCloudReader createReader(List<Point3dRgbI_F64> points) {
			var cloud = new ArrayList<Point3D_F64>();
			for (Point3dRgbI_F64 p : points) {
				cloud.add(p.copy());
			}

			return PointCloudReader.wrapF64(cloud);
		}

		@Override void checkRgb() {}
	}

	@Nested
	public class CheckWrapF32RGB extends ReaderTests {
		@Override
		public PointCloudReader createReader(List<Point3dRgbI_F64> points) {
			var cloud = new ArrayList<Point3dRgbI_F32>();
			for (Point3dRgbI_F64 p : points) {
				var c = new Point3dRgbI_F32();
				c.setTo((float) p.x, (float) p.y, (float) p.z);
				c.rgb = p.rgb;
				cloud.add(c);
			}

			return PointCloudReader.wrapF32RGB(cloud);
		}
	}

	@Nested
	public class CheckWrapF64RGB extends ReaderTests {
		@Override
		public PointCloudReader createReader(List<Point3dRgbI_F64> points) {
			return PointCloudReader.wrapF64RGB(points);
		}
	}
}
