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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.geo.h.HomographyDirectLinearTransform;
import boofcv.alg.geo.robust.GenerateHomographyLinear;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedPair3D;
import georegression.struct.ConvertFloatType;
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.transform.homography.HomographyPointOps_F32;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

/**
 * Given a set of control points, it computes a distortion model and allows the user to read the value of grid elements.
 *
 * @author Peter Abeles
 */
public class QrCodeBinaryGridToPixel {
	ModelGenerator<Homography2D_F64, AssociatedPair> generator = new GenerateHomographyLinear(true);
	HomographyDirectLinearTransform dlt = new HomographyDirectLinearTransform(true);

	DogArray<AssociatedPair> storagePairs2D = new DogArray<>(AssociatedPair::new);
	DogArray<AssociatedPair3D> storagePairs3D = new DogArray<>(AssociatedPair3D::new);

	List<AssociatedPair> pairs2D = new ArrayList<>();

	DogArray<Point2D_F64> adjustments = new DogArray<>(Point2D_F64::new);

	Homography2D_F64 H = new Homography2D_F64();
	Homography2D_F64 Hinv = new Homography2D_F64();
	Homography2D_F32 Hinv32 = new Homography2D_F32();
	Homography2D_F32 H32 = new Homography2D_F32();

	Point2D_F64 tmp64 = new Point2D_F64();

	boolean adjustWithFeatures;

	public void setTransformFromSquare( Polygon2D_F64 square ) {
		adjustWithFeatures = false;
		storagePairs2D.reset();
		pairs2D.clear();

		set(0, 0, square, 0);
		set(0, 7, square, 1);
		set(7, 7, square, 2);
		set(7, 0, square, 3);

		computeTransform();
	}

	/**
	 * Used to estimate the image to grid coordinate system before the version is known. The top left square is
	 * used to fix the coordinate system. Then 4 lines between corners  going to other QR codes is used to
	 * make it less suspectable to errors in the first 4 corners
	 */
	public void setTransformFromLinesSquare( QrCode qr ) {
		// clear old points
		storagePairs2D.reset();
		storagePairs3D.reset();

		// use 3 of the corners to set the coordinate system
//		set(0, 0, qr.ppCorner,0); <-- prone to damage. Significantly degrades results if used
		set(0, 7, qr.ppCorner, 1);
		set(7, 7, qr.ppCorner, 2);
		set(7, 0, qr.ppCorner, 3);

		// Use 4 lines to make it more robust errors in these corners
		// We just need to get the direction right for the lines. the exact grid to image doesn't matter
		setLine(0, 7, 0, 14, qr.ppCorner, 1, qr.ppRight, 0);
		setLine(7, 7, 7, 14, qr.ppCorner, 2, qr.ppRight, 3);
		setLine(7, 7, 14, 7, qr.ppCorner, 2, qr.ppDown, 1);
		setLine(7, 0, 14, 0, qr.ppCorner, 3, qr.ppDown, 0);

		DMatrixRMaj HH = new DMatrixRMaj(3, 3);
		dlt.process(storagePairs2D.toList(), storagePairs3D.toList(), null, HH);
		H.setTo(HH);
		H.invert(Hinv);
		ConvertFloatType.convert(Hinv, Hinv32);
		ConvertFloatType.convert(H, H32);
	}

	public void addAllFeatures( QrCode qr ) {
		adjustWithFeatures = false;
		storagePairs2D.reset();
		pairs2D.clear();

		int N = qr.getNumberOfModules();

		set(0, 0, qr.ppCorner, 0); // outside corner
		set(0, 7, qr.ppCorner, 1);
		set(7, 7, qr.ppCorner, 2);
		set(7, 0, qr.ppCorner, 3);

		set(0, N - 7, qr.ppRight, 0);
		set(0, N, qr.ppRight, 1); // outside corner
		set(7, N, qr.ppRight, 2);
		set(7, N - 7, qr.ppRight, 3);

		set(N - 7, 0, qr.ppDown, 0);
		set(N - 7, 7, qr.ppDown, 1);
		set(N, 7, qr.ppDown, 2);
		set(N, 0, qr.ppDown, 3); // outside corner

		for (int i = 0; i < qr.alignment.size; i++) {
			QrCode.Alignment a = qr.alignment.get(i);
			AssociatedPair p = storagePairs2D.grow();
			p.setTo(a.pixel.x, a.pixel.y, a.moduleX + 0.5f, a.moduleY + 0.5f);
			pairs2D.add(p);
		}
	}

	/**
	 * Outside corners on position patterns are more likely to be damaged, so remove them
	 */
	public void removeOutsideCornerFeatures() {
		if (pairs2D.size() != storagePairs2D.size)
			throw new RuntimeException("This can only be called when all the features have been added");

		pairs2D.remove(11);
		pairs2D.remove(5);
		pairs2D.remove(0);
	}

	public boolean removeFeatureWithLargestError() {
		int selected = -1;
		double largestError = 0;

		for (int i = 0; i < pairs2D.size(); i++) {
			AssociatedPair p = pairs2D.get(i);
			HomographyPointOps_F64.transform(Hinv, p.p2.x, p.p2.y, tmp64);
			double dx = tmp64.x - p.p1.x;
			double dy = tmp64.y - p.p1.y;

			double error = dx*dx + dy*dy;
			if (error > largestError) {
				largestError = error;
				selected = i;
			}
		}
		if (selected != -1 && largestError > 2*2) {
			pairs2D.remove(selected);
			return true;
		} else {
			return false;
		}
	}

	public void computeTransform() {
		generator.generate(pairs2D, H);
		H.invert(Hinv);
		ConvertFloatType.convert(Hinv, Hinv32);
		ConvertFloatType.convert(H, H32);

		adjustments.reset();
		if (adjustWithFeatures) {
			for (int i = 0; i < pairs2D.size(); i++) {
				AssociatedPair p = pairs2D.get(i);
				Point2D_F64 a = adjustments.grow();

				HomographyPointOps_F64.transform(Hinv, p.p2.x, p.p2.y, tmp64);
				a.x = p.p1.x - tmp64.x;
				a.y = p.p1.y - tmp64.y;
			}
		}
	}

	private void set( float row, float col, Polygon2D_F64 polygon, int corner ) {
		AssociatedPair p = storagePairs2D.grow();
		Point2D_F64 c = polygon.get(corner);
		p.setTo(c.x, c.y, col, row);
		pairs2D.add(p);
	}

	private void setLine( float row0, float col0, float row1, float col1,
						  Polygon2D_F64 polygon0, int corner0, Polygon2D_F64 polygon1, int corner1 ) {
		AssociatedPair3D p = storagePairs3D.grow();
		Point2D_F64 c0 = polygon0.get(corner0);
		Point2D_F64 c1 = polygon1.get(corner1);

		p.setTo(c1.x - c0.x, c1.y - c0.y, 0, col1 - col0, row1 - row0, 0);
		// normalize for numerical reasons. Scale of line parameters doesn't matter
		p.p1.divideIP(p.p1.norm());
		p.p2.divideIP(p.p2.norm());
	}

	public final void imageToGrid( float x, float y, Point2D_F32 grid ) {
		HomographyPointOps_F32.transform(H32, x, y, grid);
	}

	public final void imageToGrid( double x, double y, Point2D_F64 grid ) {
		HomographyPointOps_F64.transform(H, x, y, grid);
	}

	public final void gridToImage( float row, float col, Point2D_F32 pixel ) {
		HomographyPointOps_F32.transform(Hinv32, col, row, pixel);

		// Use a second adjustment based on observed features
//		// todo optimize this
		if (adjustWithFeatures) {
			int closest = -1;
			double best = Double.MAX_VALUE;
			for (int i = 0; i < pairs2D.size(); i++) {
				double d = pairs2D.get(i).p2.distance2(col, row);
				if (d < best) {
					best = d;
					closest = i;
				}
			}

			Point2D_F64 adj = adjustments.get(closest);

			pixel.x += (float)adj.x;
			pixel.y += (float)adj.y;
		}
	}

	public void setAdjustWithFeatures( boolean adjustWithFeatures ) {
		this.adjustWithFeatures = adjustWithFeatures;
	}

	public void setHomographyInv( Homography2D_F64 Hinv ) {
		this.Hinv.setTo(Hinv);
		ConvertFloatType.convert(Hinv, Hinv32);
	}
}
