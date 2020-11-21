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

package boofcv.alg.mvs;

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.LookUpColorRgbFormats;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestColorizeCloudFromImage extends BoofStandardJUnit {

	GrayU8 image = new GrayU8(1, 1);
	List<Point3D_F64> cloud3 = new ArrayList<>();
	List<Point4D_F64> cloud4 = new ArrayList<>();

	Se3_F64 world_to_view = new Se3_F64();

	CameraPinhole camera = new CameraPinhole(100, 120, 0, 200, 200, 0, 0);
	Point2Transform2_F64 norm_to_pixel;

	public TestColorizeCloudFromImage() {
		norm_to_pixel = new LensDistortionPinhole(camera).distort_F64(false, true);
		world_to_view.T.setTo(0.1, 0, 0);

		for (int i = 0; i < 20; i++) {
			cloud3.add(new Point3D_F64(rand.nextGaussian(), rand.nextGaussian(), 2.0 + rand.nextDouble()*0.5));
			Point3D_F64 p = cloud3.get(cloud3.size()-1);
			double s=1.2;
			cloud4.add(new Point4D_F64(p.x*s, p.y*s, p.z*s, s));
		}

		image.reshape(400, 406);
		ImageMiscOps.fillUniform(image, rand, 0, 200);
	}

	/** Checks nominal case with points in front of the camera */
	@Test void process3() {
		var pixel = new Point2D_F64();
		var w2c = new WorldToCameraToPixel();
		w2c.configure(camera, world_to_view);
		var alg = new ColorizeCloudFromImage<>(new LookUpColorRgbFormats.SB_U8());
		alg.process3(image, cloud3, 0, cloud3.size(), world_to_view, norm_to_pixel, ( i, r, g, b ) -> {
			w2c.transform(cloud3.get(i), pixel);
			int val = image.get((int)Math.round(pixel.x), (int)Math.round(pixel.y));
			assertEquals(val, r);
			assertEquals(val, g);
			assertEquals(val, b);
		});
	}

	/** Checks nominal case with points in front of the camera */
	@Test void process4() {
		var pixel = new Point2D_F64();
		var w2c = new WorldToCameraToPixel();
		w2c.configure(camera, world_to_view);
		var alg = new ColorizeCloudFromImage<>(new LookUpColorRgbFormats.SB_U8());
		alg.process4(image, cloud4, 0, cloud3.size(), world_to_view, norm_to_pixel, ( i, r, g, b ) -> {
			w2c.transform(cloud3.get(i), pixel);
			int val = image.get((int)Math.round(pixel.x), (int)Math.round(pixel.y));
			assertEquals(val, r);
			assertEquals(val, g);
			assertEquals(val, b);
		});
	}
}
