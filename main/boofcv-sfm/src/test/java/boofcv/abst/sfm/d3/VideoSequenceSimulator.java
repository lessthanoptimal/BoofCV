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

package boofcv.abst.sfm.d3;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Polygon2D_I32;
import org.ejml.data.DMatrixRMaj;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * @author
 */
public class VideoSequenceSimulator<I extends ImageGray<I>> extends BoofStandardJUnit {

	protected int width, height;

	Random rand = new Random(1234);

	CameraPinholeBrown intrinsic;
	DMatrixRMaj K;

	BufferedImage workImage;
	I outputImage;

	List<Square> squares = new ArrayList<>();
	protected Class<I> inputType;

	public VideoSequenceSimulator( int width, int height, Class<I> inputType ) {

		this.inputType = inputType;
		this.width = width;
		this.height = height;
		workImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
		outputImage = GeneralizedImageOps.createSingleBand(inputType, workImage.getWidth(), workImage.getHeight());
	}

	public void setIntrinsic( CameraPinholeBrown param ) {
		this.intrinsic = param;
		K = PerspectiveOps.pinholeToMatrix(param, (DMatrixRMaj)null);
	}

	protected void createSquares( int total, double minZ, double maxZ ) {
		squares.clear();

		double t = 0.1;

		Point2D_F64 n = new Point2D_F64();
		Point2Transform2_F64 tranNorm = LensDistortionFactory.narrow(intrinsic).undistort_F64(true, false);

		for (int i = 0; i < total; i++) {

			// generate the squares uniformally inside the FOV
			tranNorm.compute(
					rand.nextDouble()*(intrinsic.width - 1),
					rand.nextDouble()*(intrinsic.height - 1), n);

			double z = rand.nextDouble()*(maxZ - minZ) + minZ;
			double x = n.x*z;
			double y = n.y*z;

			Square s = new Square();
			s.a.set(x, y, z);
			s.b.set(x + t, y, z);
			s.c.set(x + t, y + t, z);
			s.d.set(x, y + t, z);

			s.gray = rand.nextInt(200) + 55;

			squares.add(s);
		}

		// sort by depth so that objects farther way are rendered first and obstructed by objects closer in view
		Collections.sort(squares, new Comparator<Square>() {
			@Override
			public int compare( Square o1, Square o2 ) {
				if (o1.a.z < o2.a.z)
					return -1;
				if (o1.a.z > o2.a.z)
					return 1;
				else
					return 0;
			}
		});
	}

	public I render( Se3_F64 worldToCamera ) {
		GImageMiscOps.fill(outputImage, 0);

		for (Square s : squares) {
			Point2D_F64 p1 = PerspectiveOps.renderPixel(worldToCamera, K, s.a, null);
			Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCamera, K, s.b, null);
			Point2D_F64 p3 = PerspectiveOps.renderPixel(worldToCamera, K, s.c, null);
			Point2D_F64 p4 = PerspectiveOps.renderPixel(worldToCamera, K, s.d, null);

			if (p1 == null || p2 == null || p3 == null || p4 == null)
				continue;

			Polygon2D_I32 p = new Polygon2D_I32(4);
			p.vertexes.data[0].setTo((int)p1.x, (int)p1.y);
			p.vertexes.data[1].setTo((int)p2.x, (int)p2.y);
			p.vertexes.data[2].setTo((int)p3.x, (int)p3.y);
			p.vertexes.data[3].setTo((int)p4.x, (int)p4.y);

			convexFill(p, outputImage, s.gray);
		}

		// TODO apply lens distortion
		return outputImage;
	}

	public void renderDepth( Se3_F64 worldToCamera, ImageGray depthImage, double units ) {

		GImageMiscOps.fill(depthImage, 0);
		for (Square s : squares) {
			Point2D_F64 p1 = PerspectiveOps.renderPixel(worldToCamera, K, s.a, null);
			Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCamera, K, s.b, null);
			Point2D_F64 p3 = PerspectiveOps.renderPixel(worldToCamera, K, s.c, null);
			Point2D_F64 p4 = PerspectiveOps.renderPixel(worldToCamera, K, s.d, null);

			if (p1 == null || p2 == null || p3 == null || p4 == null)
				continue;

			Polygon2D_I32 p = new Polygon2D_I32(4);
			p.vertexes.data[0].setTo((int)p1.x, (int)p1.y);
			p.vertexes.data[1].setTo((int)p2.x, (int)p2.y);
			p.vertexes.data[2].setTo((int)p3.x, (int)p3.y);
			p.vertexes.data[3].setTo((int)p4.x, (int)p4.y);

			int depth = (int)(s.a.z/units);

			convexFill(p, depthImage, depth);
		}
		// TODO apply lens distortion
	}

	private void convexFill( Polygon2D_I32 poly, ImageGray image, double value ) {
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (int i = 0; i < poly.size(); i++) {
			Point2D_I32 p = poly.vertexes.data[i];
			if (p.y < minY) {
				minY = p.y;
			} else if (p.y > maxY) {
				maxY = p.y;
			}
			if (p.x < minX) {
				minX = p.x;
			} else if (p.x > maxX) {
				maxX = p.x;
			}
		}
		ImageRectangle bounds = new ImageRectangle(minX, minY, maxX, maxY);
		BoofMiscOps.boundRectangleInside(image, bounds);

		Point2D_F64 p = new Point2D_F64();
		Polygon2D_F64 poly64 = new Polygon2D_F64(4);
		for (int i = 0; i < 4; i++)
			poly64.vertexes.data[i].setTo(poly.vertexes.data[i].x, poly.vertexes.data[i].y);

		for (int y = bounds.y0; y < bounds.y1; y++) {
			p.y = y;
			for (int x = bounds.x0; x < bounds.x1; x++) {
				p.x = x;

				if (Intersection2D_F64.containsConvex(poly64, p)) {
					GeneralizedImageOps.set(image, x, y, value);
				}
			}
		}
	}

	protected static class Square {
		int gray;
		public Point3D_F64 a = new Point3D_F64();
		public Point3D_F64 b = new Point3D_F64();
		public Point3D_F64 c = new Point3D_F64();
		public Point3D_F64 d = new Point3D_F64();
	}
}
