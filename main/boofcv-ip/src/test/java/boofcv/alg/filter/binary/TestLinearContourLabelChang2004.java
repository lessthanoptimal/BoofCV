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

package boofcv.alg.filter.binary;

import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.struct.ConnectRule;
import boofcv.struct.PackedSetsPoint2D_I32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestLinearContourLabelChang2004 {

	public static GrayU8 TEST1 = new GrayU8(new byte[][]
						  {{0,0,0,0,0,0,0,1,0,0,0,1,1},
						   {0,0,0,0,0,0,0,1,0,0,0,1,1},
						   {0,0,0,0,0,0,0,1,0,0,1,1,0},
						   {0,0,0,0,0,0,0,0,1,1,1,1,0},
						   {0,0,1,0,0,0,0,0,1,1,1,0,0},
						   {0,0,1,0,0,0,1,1,1,1,1,0,0},
						   {1,1,1,1,1,1,1,1,1,1,0,0,0},
						   {0,0,0,1,1,1,1,1,0,0,0,0,0}});

	public static GrayU8 TEST2 = new GrayU8(new byte[][]
				   {{0,0,1,0,0,0,0,1,0,0,0,0,0},
					{0,1,0,1,0,0,1,0,0,1,0,0,0},
					{0,0,1,0,0,1,0,1,0,1,1,1,0},
					{0,0,0,0,1,0,0,0,1,1,1,1,0},
					{0,0,1,0,1,0,0,0,1,0,0,0,0},
					{0,0,0,0,1,0,1,1,1,0,1,1,0},
					{1,1,1,0,0,1,0,0,1,0,0,1,0},
					{0,0,0,1,1,1,1,1,0,0,0,0,0}});

	public static GrayU8 TEST3 = new GrayU8(new byte[][]
					{{0,0,0,0,0},
					 {0,1,1,1,0},
					 {0,1,1,1,0},
					 {0,1,0,1,0},
					 {0,1,1,1,0},
					 {0,0,0,0,0}});

	public static GrayU8 TEST4 = new GrayU8(new byte[][]
					{{0,0,0,0,0,0,0},
					 {0,0,1,1,1,1,1},
					 {0,1,0,1,1,1,1},
					 {0,1,1,1,0,1,1},
					 {0,1,1,1,1,1,1},
					 {0,1,1,1,1,1,1},
					 {0,1,1,1,1,1,1},
					 {0,0,0,0,0,0,0}});


	List<Point2D_I32> local;

	public TestLinearContourLabelChang2004() {
		local = new ArrayList<>();
		local.add(new Point2D_I32(-1,-1));
		local.add(new Point2D_I32( 0,-1));
		local.add(new Point2D_I32( 1,-1));
		local.add(new Point2D_I32( 1, 0));
		local.add(new Point2D_I32( 1, 1));
		local.add(new Point2D_I32( 0, 1));
		local.add(new Point2D_I32(-1, 1));
		local.add(new Point2D_I32(-1, 0));
		local.add(new Point2D_I32(-1, -1));
	}

	@Test
	public void test1_4() {
		GrayU8 input = TEST1.clone();

		GrayS32 labeled = new GrayS32(input.width,input.height);
		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004(ConnectRule.FOUR);
		alg.process(input, labeled);

		assertEquals(2, alg.getContours().size);
		checkContour(alg, labeled, 4);
	}

	@Test
	public void test1_8() {
		GrayU8 input = TEST1.clone();

		GrayS32 labeled = new GrayS32(input.width,input.height);
		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004(ConnectRule.EIGHT);
		alg.process(input, labeled);

		assertEquals(1, alg.getContours().size);
		checkContour(alg, labeled,8);
	}

	@Test
	public void test2_4() {
		GrayU8 input = TEST2.clone();

		GrayS32 labeled = new GrayS32(input.width,input.height);
		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004(ConnectRule.FOUR);
		alg.process(input,labeled);

		assertEquals(14,alg.getContours().size);
		checkContour(alg, labeled,4);
	}

	@Test
	public void test2_8() {
		GrayU8 input = TEST2.clone();

		GrayS32 labeled = new GrayS32(input.width,input.height);
		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004(ConnectRule.EIGHT);
		alg.process(input,labeled);

		assertEquals(4,alg.getContours().size);
		checkContour(alg, labeled,8);
	}

	@Test
	public void test3_4() {
		GrayU8 input = TEST4.clone();

		GrayS32 labeled = new GrayS32(input.width,input.height);
		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004(ConnectRule.FOUR);
		alg.process(input,labeled);

		assertEquals(1, alg.getContours().size);
		checkContour(alg, labeled,4);
	}

	@Test
	public void test3_8() {
		GrayU8 input = TEST4.clone();

		GrayS32 labeled = new GrayS32(input.width,input.height);
		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004(ConnectRule.EIGHT);
		alg.process(input, labeled);

		assertEquals(1,alg.getContours().size);
		checkContour(alg, labeled,8);
	}

	/**
	 * Check to see if inner and outer contours are being computed correctly
	 */
	@Test
	public void checkInnerOuterContour() {
		GrayU8 input = TEST3.clone();

		GrayS32 labeled = new GrayS32(input.width,input.height);
		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004(ConnectRule.EIGHT);
		alg.process(input,labeled);

		assertEquals(1,alg.getContours().size);
		checkContour(alg, labeled,8);

		ContourPacked c = alg.getContours().get(0);
		assertEquals(10, alg.packedPoints.sizeOfSet(c.externalIndex));
		assertEquals(1, c.internalIndexes.size);
		assertEquals(4, alg.packedPoints.sizeOfSet(c.externalIndex +1));
	}

	/**
	 * Creates a list of every pixel with the specified label that is on the contour.  Removes duplicate points
	 * in the found contour.  Sees if the two lists are equivalent.
	 *
	 * @param rule Which connectivity rule is being tested
	 */
	private void checkContour(LinearContourLabelChang2004 alg, GrayS32 labeled , int rule ) {

		FastQueue<ContourPacked> contours = alg.getContours();

		for( int i = 0; i < contours.size(); i++ ) {
//			System.out.println("=================== Contour "+i);
			ContourPacked c = contours.get(i);

			assertTrue(c.id > 0 );

			List<Point2D_I32> found = new ArrayList<>();
			addPointsToList(alg,c.externalIndex,found);
			for(int j = 0; j < c.internalIndexes.size; j++ ) {
				addPointsToList(alg,c.internalIndexes.get(j),found);
			}

			// there can be duplicate points, remove them
			found = removeDuplicates(found);

			// see if the two lists are equivalent
			List<Point2D_I32> expected = rule == 8 ? findContour8(labeled, c.id) : findContour4(labeled, c.id);

//			labeled.print();
//			System.out.println("------------------");
//			print(found,labeled.width,labeled.height);
//			print(expected,labeled.width,labeled.height);

			assertEquals(expected.size(),found.size());

			for( Point2D_I32 f : found ) {
				boolean match = false;
				for( Point2D_I32 e : expected ) {
					if( f.x == e.x && f.y == e.y ) {
						match = true;
						break;
					}
				}
				assertTrue(match);
			}
		}
	}

	private void addPointsToList( LinearContourLabelChang2004 alg , int set , List<Point2D_I32> list ) {
		PackedSetsPoint2D_I32.SetIterator iter = alg.packedPoints.createIterator();
		iter.setup(set);
		while( iter.hasNext() ) {
			list.add( iter.next().copy() );
		}
	}

	/**
	 * Create an unordered list of all points in the internal and external contour
	 */
	private List<Point2D_I32> findContour8(GrayS32 labeled, int target) {
		List<Point2D_I32> list = new ArrayList<>();

		ImageBorder<GrayS32> border = FactoryImageBorder.singleValue(labeled, 0);

		for( int y = 0; y < labeled.height; y++ ) {
			for( int x = 0; x < labeled.width; x++ ) {
				if( target == labeled.get(x,y) ) {

					boolean isContour = false;
					for( int i = 0; i < local.size()-1; i++ ) {
						Point2D_I32 a = local.get(i);
						Point2D_I32 b = local.get(i+1);

						if( get(border, x + a.x, y + a.y) != target && get(border, x + b.x, y + b.y) != target ) {
							isContour = true;
							break;
						}
					}

					if( !isContour && get(border, x + 1, y) != target)
						isContour = true;
					if( !isContour && get(border, x - 1, y) != target)
						isContour = true;
					if( !isContour && get(border, x, y + 1) != target)
						isContour = true;
					if( !isContour && get(border, x, y - 1) != target)
						isContour = true;

					if( isContour )
						list.add( new Point2D_I32(x,y));
				}
			}
		}
		return list;
	}

	private float get(ImageBorder<GrayS32> border , int x , int y ) {
		double tmp0[] = new double[1];
		border.getGeneral(x, y, tmp0);
		return (float)tmp0[0];
	}

	/**
	 * Create an unordered list of all points in the internal and external contour
	 */
	private List<Point2D_I32> findContour4(GrayS32 labeled, int target) {
		List<Point2D_I32> list = new ArrayList<>();

		ImageBorder<GrayS32> border = FactoryImageBorder.singleValue(labeled, 0);

		for( int y = 0; y < labeled.height; y++ ) {
			for( int x = 0; x < labeled.width; x++ ) {
				if( target == labeled.get(x,y) ) {

					boolean isContour = false;
					for( int i = 0; i < local.size(); i++ ) {
						Point2D_I32 a = local.get(i);
						if( get(border, x + a.x, y + a.y) != target ) {
							isContour = true;
						}
					}

					if( isContour )
						list.add( new Point2D_I32(x,y));
				}
			}
		}
		return list;
	}

	private List<Point2D_I32> removeDuplicates( List<Point2D_I32> list ) {
		List<Point2D_I32> ret = new ArrayList<>();

		for( int i = 0; i < list.size(); i++ ) {
			Point2D_I32 p = list.get(i);
			boolean matched = false;
			for( int j = i+1; j < list.size(); j++ ) {
				Point2D_I32 c = list.get(j);
				if( p.x == c.x && p.y == c.y ) {
					matched = true;
					break;
				}
			}
			if( !matched ) {
				ret.add(p);
			}
		}
		return ret;
	}

	private void print( List<Point2D_I32> l , int w, int h ) {
		GrayU8 img = new GrayU8(w,h);

		for( Point2D_I32 p : l ) {
			img.set(p.x,p.y,1);
		}
		img.print();
		System.out.println("------------------");
	}
}
