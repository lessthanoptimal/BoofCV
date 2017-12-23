/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.geo.robust.GenerateHomographyLinear;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.ConvertFloatType;
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.transform.homography.HomographyPointOps_F32;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.struct.FastQueue;

/**
 * Given a set of control points, it computes a distortion model and allows the user to read the value of grid elements.
 *
 * @author Peter Abeles
 */
public class QrCodeBinaryGridToPixel {
	ModelGenerator<Homography2D_F64,AssociatedPair> generator = new GenerateHomographyLinear(true);

	FastQueue<AssociatedPair> pairs = new FastQueue<>(AssociatedPair.class,true);
	FastQueue<Point2D_F64> adjustments = new FastQueue<>(Point2D_F64.class,true);

	Homography2D_F64 H = new Homography2D_F64();
	Homography2D_F64 Hinv = new Homography2D_F64();
	Homography2D_F32 Hinv32 = new Homography2D_F32();

	public void setMarker( QrCode qr ) {
		int N = qr.totalModules();

		pairs.reset();
		set(0, 7, qr.ppCorner,1);
		set(7, 7, qr.ppCorner,2);
		set(7, 0, qr.ppCorner,3);

		set(0, N-7, qr.ppRight,0);
		set(7, N-7, qr.ppRight,3);
		set(7, N, qr.ppRight,2);

		set(N-7, 0, qr.ppDown,0);
		set(N-7, 7, qr.ppDown,1);
		set(N, 7, qr.ppDown,2);

		for (int i = 0; i < qr.alignment.size; i++) {
			QrCode.Alignment a = qr.alignment.get(i);
			pairs.grow().set(a.pixel.x,a.pixel.y,a.moduleX+0.5f,a.moduleY+0.5f);
		}

		generator.generate(pairs.toList(),H);
		H.invert(Hinv);
		ConvertFloatType.convert(Hinv, Hinv32);

		Point2D_F64 tmp = new Point2D_F64();
		adjustments.reset();
		for (int i = 0; i < pairs.size; i++) {
			AssociatedPair p = pairs.get(i);
			Point2D_F64 a = adjustments.grow();

			HomographyPointOps_F64.transform(Hinv,p.p2.x,p.p2.y,tmp);
			a.x = p.p1.x-tmp.x;
			a.y = p.p1.y-tmp.y;
		}
	}

	private void set(float row, float col, Polygon2D_F64 polygon, int corner) {
		Point2D_F64 c = polygon.get(corner);

		pairs.grow().set(c.x,c.y,col,row);

	}

	public final void gridToImage(float row, float col, Point2D_F32 pixel) {
		HomographyPointOps_F32.transform(Hinv32,col,row,pixel);

		// Use a second adjustment based on observed features
		// todo optimize this
		int closest = -1;
		double best = Double.MAX_VALUE;
		for (int i = 0; i < pairs.size; i++) {
			double d = pairs.get(i).p2.distance2(row,col);
			if( d < best ) {
				best = d;
				closest = i;
			}
		}

		Point2D_F64 adj = adjustments.get(closest);

		pixel.x += adj.x;
		pixel.y += adj.y;
	}

}
