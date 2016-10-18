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

package boofcv.alg.shapes.polygon;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.se.Se2_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.transform.ConvertTransform_F64;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRefinePolygonCornersToImage extends BaseFitPolygon{

	List<Point2D_I32> contour;
	GrowQueue_I32 split;

	@Test
	public void perfectRectangle() {

		Polygon2D_F64 expected = new Polygon2D_F64(x0,y0 , x1,y0, x1,y1, x0,y1);
		Polygon2D_F64 found = new Polygon2D_F64(4);

		for (Class imageType : imageTypes) {
			for (int i = 0; i < 2; i++) {
				boolean black = i == 0;

				setup(new Affine2D_F64(), black, imageType);
				findContour(black);

				RefinePolygonCornersToImage alg = new RefinePolygonCornersToImage(imageType);

				alg.setImage(image);
				assertTrue(alg.refine(null, contour, split, found));

				assertTrue(expected.isEquivalent(found, 0.01));
			}
		}
	}

	@Test
	public void perfectRectangle_subimage() {
		Polygon2D_F64 expected = new Polygon2D_F64(x0,y0 , x1,y0, x1,y1, x0,y1);
		Polygon2D_F64 found = new Polygon2D_F64(4);

		for (Class imageType : imageTypes) {
			for (int i = 0; i < 2; i++) {
				boolean black = i == 0;

				setup(new Affine2D_F64(), black, imageType);
				findContour(black);

				RefinePolygonCornersToImage alg = new RefinePolygonCornersToImage(imageType);

				alg.setImage(BoofTesting.createSubImageOf_S(image));
				assertTrue(alg.refine(null, contour, split, found));

				assertTrue(expected.isEquivalent(found, 0.01));
			}
		}
	}

	/**
	 * See if it handles lines along the image border correctly
	 */
	@Test
	public void fitWithEdgeOnBorder() {
		for (Class imageType : imageTypes) {
			x0 = 0; x1 = 100;
			y0 = 100; y1 = 200;
			setup(null, true, imageType);

			RefinePolygonCornersToImage alg = new RefinePolygonCornersToImage(imageType);

			findContour(true);

			Polygon2D_F64 found = new Polygon2D_F64(4);

			alg.setImage(image);
			assertTrue(alg.refine(null, contour, split,found));

			Polygon2D_F64 expected = createFromSquare(null);
			expected.flip();
			assertTrue(expected.isEquivalent(found, 0.01));
		}
	}

	@Test
	public void distortedRectangle() {
		double tol = 0.4;
		Polygon2D_F64 original = new Polygon2D_F64(x0,y0 , x1,y0, x1,y1, x0,y1);
		Polygon2D_F64 found = new Polygon2D_F64(4);

		Affine2D_F64 affines[] = new Affine2D_F64[2];
		affines[0] = new Affine2D_F64();
		affines[1] = new Affine2D_F64(1.3,0.05,-0.15,0.87,0.1,0.6);
		ConvertTransform_F64.convert(new Se2_F64(0, 0, 0.2), affines[0]);

		for (Class imageType : imageTypes) {
			for (int i = 0; i < 2; i++) {
				for(Affine2D_F64 affine : affines ) {
					boolean black = i == 0;

					setup(affine, black, imageType);
					findContour(black);

					RefinePolygonCornersToImage alg = new RefinePolygonCornersToImage( imageType);

					alg.setImage(BoofTesting.createSubImageOf_S(image));
					assertTrue(alg.refine(null, contour, split, found));

					Polygon2D_F64 expected = apply(affine,original);
					assertTrue(expected.isEquivalent(found, tol));
				}
			}
		}
	}

	@Test
	public void pickEndIndex() {
		RefinePolygonCornersToImage alg = new RefinePolygonCornersToImage(GrayU8.class);

		alg.setPixelsAway(6);
		alg.contour = new ArrayList();
		alg.splits = new GrowQueue_I32();

		int away = alg.getPixelsAway();
		int N = 20;
		for (int i = 0; i < N; i++) {
			alg.contour.add( new Point2D_I32());
		}

		alg.splits.add(0);
		alg.splits.add(7);
		alg.splits.add(9);
		alg.splits.add(14);

		assertEquals(away, alg.pickEndIndex(0,1));
		assertEquals(14  , alg.pickEndIndex(0,-1));
		assertEquals(9  , alg.pickEndIndex(1, 1));
		assertEquals(1  , alg.pickEndIndex(1,-1));
		assertEquals(14 , alg.pickEndIndex(2, 1));
		assertEquals(7  , alg.pickEndIndex(2,-1));
		assertEquals(0  , alg.pickEndIndex(3, 1));
		assertEquals(9  , alg.pickEndIndex(3,-1));
	}

	private void findContour( boolean black ) {
		GrayU8 binary = new GrayU8(image.width,image.height);
		GThresholdImageOps.threshold(image,binary,40,black);

		contour = BinaryImageOps.contour(binary, ConnectRule.FOUR,null).get(0).external;

		List<PointIndex_I32> corners = ShapeFittingOps.fitPolygon(contour,true,0.05,0.1,10);

		split = new GrowQueue_I32(corners.size());
		for (int i = 0; i < corners.size(); i++) {
			split.add(corners.get(i).index);
		}
	}

	/**
	 * Reproduces a bug that was found in the wild where the same index was returned in both directions.  It was
	 * caused by the distance being incorrectly computed.
	 */
	@Test
	public void pickEndIndex_bug0() {
		RefinePolygonCornersToImage<GrayU8> alg = new RefinePolygonCornersToImage<>(GrayU8.class);
		alg.setPixelsAway(6);
		alg.contour = new ArrayList<>();
		alg.splits = new GrowQueue_I32();

		for (int i = 0; i < 100; i++) {
			alg.contour.add(new Point2D_I32(1,1));
		}
		alg.splits.add(80);
		alg.splits.add(95);
		alg.splits.add(3);
		alg.splits.add(20);

		int found0 = alg.pickEndIndex(0,-1);
		int found1 = alg.pickEndIndex(0,1);

		assertTrue(found0!=found1);

		// quick test in the other direction
		alg.splits.reset();
		alg.splits.add(20);
		alg.splits.add(3);
		alg.splits.add(95);
		alg.splits.add(80);

		int foundA = alg.pickEndIndex(3,1);
		int foundB = alg.pickEndIndex(3,-1);

		assertTrue(foundA!=foundB);
		assertEquals(foundA,found0);
		assertEquals(foundB,found1);
	}
}
