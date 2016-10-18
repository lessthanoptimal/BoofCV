/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.depth;

import boofcv.alg.distort.radtan.RemoveRadialPtoN_F64;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.FastQueueArray_I32;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestVisualDepthOps {
	Random rand = new Random(234);
	int width = 640;
	int height = 480;

	CameraPinholeRadial param = new CameraPinholeRadial(200,201,0,width/2,height/2,width,height).fsetRadial(0,0);

	@Test
	public void depthTo3D() {

		GrayU16 depth = new GrayU16(width,height);

		depth.set(200,80,3400);
		depth.set(600,420,50);

		FastQueue<Point3D_F64> pts = new FastQueue<>(Point3D_F64.class, true);

		VisualDepthOps.depthTo3D(param,depth,pts);

		assertEquals(2,pts.size());

		assertEquals(0,compute(200,80,3400).distance(pts.get(0)),1e-8);
		assertEquals(0,compute(600,420,50).distance(pts.get(1)),1e-8);
	}

	@Test
	public void depthTo3D_with_rgb() {
		GrayU16 depth = new GrayU16(width,height);

		depth.set(200,80,3400);
		depth.set(600,420,50);

		Planar<GrayU8> rgb = new Planar<>(GrayU8.class,width,height,3);
		GImageMiscOps.fillUniform(rgb, rand, 0, 200);

		FastQueue<Point3D_F64> pts = new FastQueue<>(Point3D_F64.class, true);
		FastQueueArray_I32 color = new FastQueueArray_I32(3);

		VisualDepthOps.depthTo3D(param,rgb,depth,pts,color);

		assertEquals(2,pts.size());
		assertEquals(2,color.size());

		assertEquals(0,compute(200,80,3400).distance(pts.get(0)),1e-8);
		assertEquals(0, compute(600, 420, 50).distance(pts.get(1)), 1e-8);

		color(200,80,rgb,color.get(0));
		color(600, 420,rgb,color.get(1));
	}

	private void color(int x , int y , Planar<GrayU8> rgb, int found[] ) {

		assertEquals(rgb.getBand(0).get(x,y),found[0]);
		assertEquals(rgb.getBand(1).get(x,y),found[1]);
		assertEquals(rgb.getBand(2).get(x,y),found[2]);
	}

	private Point3D_F64 compute( int x , int y , int distance ) {
		Point2D_F64 n = new Point2D_F64();

		RemoveRadialPtoN_F64 p2n = new RemoveRadialPtoN_F64();
		p2n.setK(param.fx,param.fy,param.skew,param.cx,param.cy).setDistortion(param.radial,param.t1,param.t2);

		p2n.compute(x,y,n);

		Point3D_F64 p = new Point3D_F64();
		p.z = distance;
		p.x = n.x*p.z;
		p.y = n.y*p.z;

		return p;
	}


}
