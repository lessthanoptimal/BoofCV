/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPointCloudUtils {

	Random rand = new Random(234);

	@Test
	public void autoScale() {
		Point3D_F64 mean = new Point3D_F64(1,2,3);
		Point3D_F64 stdev = new Point3D_F64(2,0.5,0.1);

		List<Point3D_F64> list = new ArrayList<>();

		for (int i = 0; i < 10000; i++) {
			Point3D_F64 p = new Point3D_F64();
			p.x = rand.nextGaussian()*stdev.x + mean.x;
			p.y = rand.nextGaussian()*stdev.y + mean.y;
			p.z = rand.nextGaussian()*stdev.z + mean.z;
			list.add(p);
		}

		double scale = PointCloudUtils.autoScale(list,10);

		Point3D_F64 foundMean = new Point3D_F64();
		Point3D_F64 foundStdev = new Point3D_F64();
		PointCloudUtils.statistics(list,foundMean,foundStdev);

		foundMean.scale(1.0/scale);
		assertEquals(0,foundMean.distance(mean), 0.01);

		double maxStdev = Math.max(Math.max(foundStdev.x,foundStdev.y),foundStdev.z);

		assertEquals(10,maxStdev, 0.01);
	}

	@Test
	public void statistics() {
		Point3D_F64 mean = new Point3D_F64(1,2,3);
		Point3D_F64 stdev = new Point3D_F64(2,0.5,0.1);

		List<Point3D_F64> list = new ArrayList<>();

		for (int i = 0; i < 10000; i++) {
			Point3D_F64 p = new Point3D_F64();
			p.x = rand.nextGaussian()*stdev.x + mean.x;
			p.y = rand.nextGaussian()*stdev.y + mean.y;
			p.z = rand.nextGaussian()*stdev.z + mean.z;
			list.add(p);
		}

		Point3D_F64 foundMean = new Point3D_F64();
		Point3D_F64 foundStdev = new Point3D_F64();

		PointCloudUtils.statistics(list,foundMean,foundStdev);

		assertEquals(0,foundMean.distance(mean), 0.01);
		assertEquals(0,foundStdev.distance(stdev), 0.01);
	}

	@Test
	public void prune() {
		List<Point3D_F64> list = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			list.add( new Point3D_F64(i*0.1,0,0));
		}

		PointCloudUtils.prune(list,3,0.31);
		assertEquals(100,list.size());

		// end points should be pruned
		PointCloudUtils.prune(list,3,0.28);
		assertEquals(98,list.size());
	}

	@Test
	public void prune_color() {
		List<Point3D_F64> list = new ArrayList<>();
		GrowQueue_I32 rgb = new GrowQueue_I32();
		for (int i = 0; i < 100; i++) {
			list.add( new Point3D_F64(i*0.1,0,0));
			rgb.add(i);
		}

		PointCloudUtils.prune(list,rgb,3,0.31);
		assertEquals(100,list.size());
		assertEquals(100,rgb.size());

		// end points should be pruned
		PointCloudUtils.prune(list,rgb,3,0.28);
		assertEquals(98,list.size());
		assertEquals(98,rgb.size());
		assertEquals(1,rgb.get(0));
		assertEquals(97,rgb.get(96));
	}
}