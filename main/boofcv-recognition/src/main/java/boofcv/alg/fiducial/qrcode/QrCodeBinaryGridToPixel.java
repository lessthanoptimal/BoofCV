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

import java.util.ArrayList;
import java.util.List;

/**
 * Given a set of control points, it computes a distortion model and allows the user to read the value of grid elements.
 *
 * @author Peter Abeles
 */
public class QrCodeBinaryGridToPixel {
	ModelGenerator<Homography2D_F64,AssociatedPair> generator = new GenerateHomographyLinear(true);

	FastQueue<AssociatedPair> storagePairs = new FastQueue<>(AssociatedPair.class,true);

	List<AssociatedPair> pairs = new ArrayList<>();
	FastQueue<Point2D_F64> adjustments = new FastQueue<>(Point2D_F64.class,true);

	Homography2D_F64 H = new Homography2D_F64();
	Homography2D_F64 Hinv = new Homography2D_F64();
	Homography2D_F32 Hinv32 = new Homography2D_F32();
	Homography2D_F32 H32 = new Homography2D_F32();

	Point2D_F64 tmp64 = new Point2D_F64();

	boolean adjustWithFeatures;

	public void setTransformFromSquare( Polygon2D_F64 square ) {
		adjustWithFeatures = false;
		storagePairs.reset();
		pairs.clear();

		set(0, 0, square,0);
		set(0, 7, square,1);
		set(7, 7, square,2);
		set(7, 0, square,3);

		computeTransform();
	}

	public void addAllFeatures( QrCode qr ) {
		adjustWithFeatures = false;
		storagePairs.reset();
		pairs.clear();

		int N = qr.getNumberOfModules();

		set(0, 0, qr.ppCorner,0); // outside corner
		set(0, 7, qr.ppCorner,1);
		set(7, 7, qr.ppCorner,2);
		set(7, 0, qr.ppCorner,3);

		set(0, N-7, qr.ppRight,0);
		set(0, N, qr.ppRight,1); // outside corner
		set(7, N, qr.ppRight,2);
		set(7, N-7, qr.ppRight,3);

		set(N-7, 0, qr.ppDown,0);
		set(N-7, 7, qr.ppDown,1);
		set(N, 7, qr.ppDown,2);
		set(N, 0, qr.ppDown,3); // outside corner

		for (int i = 0; i < qr.alignment.size; i++) {
			QrCode.Alignment a = qr.alignment.get(i);
			AssociatedPair p = storagePairs.grow();
			p.set(a.pixel.x,a.pixel.y,a.moduleX+0.5f,a.moduleY+0.5f);
			pairs.add(p);
		}
	}

	/**
	 * Outside corners on position patterns are more likely to be damaged, so remove them
	 */
	public void removeOutsideCornerFeatures() {
		if( pairs.size() != storagePairs.size )
			throw new RuntimeException("This can only be called when all the features have been added");

		pairs.remove(11);
		pairs.remove(5);
		pairs.remove(0);
	}

	public boolean removeFeatureWithLargestError() {
		int selected = -1;
		double largestError = 0;

		for (int i = 0; i < pairs.size(); i++) {
			AssociatedPair p = pairs.get(i);
			HomographyPointOps_F64.transform(Hinv,p.p2.x,p.p2.y,tmp64);
			double dx = tmp64.x - p.p1.x;
			double dy = tmp64.y - p.p1.y;

			double error = dx*dx + dy*dy;
			if( error > largestError ) {
				largestError = error;
				selected = i;
			}
		}
		if( selected != -1 && largestError > 2*2 ) {
			pairs.remove(selected);
			return true;
		} else {
			return false;
		}
	}

	public void computeTransform() {
		generator.generate(pairs,H);
		H.invert(Hinv);
		ConvertFloatType.convert(Hinv, Hinv32);
		ConvertFloatType.convert(H, H32);

		adjustments.reset();
		if( adjustWithFeatures ) {
			for (int i = 0; i < pairs.size(); i++) {
				AssociatedPair p = pairs.get(i);
				Point2D_F64 a = adjustments.grow();

				HomographyPointOps_F64.transform(Hinv, p.p2.x, p.p2.y, tmp64);
				a.x = p.p1.x - tmp64.x;
				a.y = p.p1.y - tmp64.y;
			}
		}
	}

	private void set(float row, float col, Polygon2D_F64 polygon, int corner) {
		AssociatedPair p = storagePairs.grow();
		Point2D_F64 c = polygon.get(corner);
		p.set(c.x,c.y,col,row);
		pairs.add(p);
	}

	public final void imageToGrid(float x, float y, Point2D_F32 grid) {
		HomographyPointOps_F32.transform(H32, x, y, grid);
	}

	public final void imageToGrid(double x, double y, Point2D_F64 grid) {
		HomographyPointOps_F64.transform(H, x, y, grid);
	}

	public final void gridToImage(float row, float col, Point2D_F32 pixel) {
		HomographyPointOps_F32.transform(Hinv32, col, row, pixel);

		// Use a second adjustment based on observed features
//		// todo optimize this
		if (adjustWithFeatures) {
			int closest = -1;
			double best = Double.MAX_VALUE;
			for (int i = 0; i < pairs.size(); i++) {
				double d = pairs.get(i).p2.distance2(col, row);
				if (d < best) {
					best = d;
					closest = i;
				}
			}

			Point2D_F64 adj = adjustments.get(closest);

			pixel.x += adj.x;
			pixel.y += adj.y;
		}
	}

	public void setAdjustWithFeatures(boolean adjustWithFeatures) {
		this.adjustWithFeatures = adjustWithFeatures;
	}

	public void setHomographyInv(Homography2D_F64 Hinv) {
		this.Hinv.set(Hinv);
		ConvertFloatType.convert(Hinv, Hinv32);
	}
}
