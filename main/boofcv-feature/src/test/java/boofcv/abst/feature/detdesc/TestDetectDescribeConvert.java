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

package boofcv.abst.feature.detdesc;

import boofcv.abst.feature.convert.ConvertTupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.feature.TupleDesc_U8;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 **/
public class TestDetectDescribeConvert extends BoofStandardJUnit {
	/**
	 * Makes sure all the other functions have been passed through
	 */
	@Test void passedThrough() {
		var alg = new DetectDescribeConvertTuple<>(new DummyDD(), new MockConvert());

		assertEquals(1, alg.createDescription().size());
		assertSame(TupleDesc_U8.class, alg.getDescriptionType());
		assertSame(GrayU8.class, alg.getInputType().getImageClass());
		assertEquals(2, alg.getNumberOfSets());
		assertEquals(5, alg.getNumberOfFeatures());
		for (int i = 0; i < 5; i++) {
			assertEquals((double)i, alg.getDescription(i).get(0));
			assertEquals(i, alg.getLocation(i).x);
			assertEquals(i, alg.getLocation(i).y);
			assertEquals(2.0, alg.getRadius(i));
			assertEquals(1.0, alg.getOrientation(i));
		}
		assertTrue(alg.hasScale());
		assertTrue(alg.hasOrientation());

		alg.detect(new GrayU8(1,1));
	}

	/** Minimal test class with known results to make sure pass through is working */
	private static class DummyDD implements DetectDescribePoint<GrayU8, TupleDesc_F64> {
		boolean detect = false;

		// @formatter:off
		@Override public TupleDesc_F64 createDescription() {return new TupleDesc_F64(1);}
		@Override public Class<TupleDesc_F64> getDescriptionType() {return TupleDesc_F64.class;}
		@Override public TupleDesc_F64 getDescription( int index ) {return new TupleDesc_F64((double)index);}
		@Override public int getNumberOfSets() {return 2;}
		@Override public int getSet( int index ) {return index%2;}
		@Override public int getNumberOfFeatures() {return 5;}
		@Override public Point2D_F64 getLocation( int featureIndex ) {return new Point2D_F64(featureIndex, featureIndex);}
		@Override public double getRadius( int featureIndex ) {return 2;}
		@Override public double getOrientation( int featureIndex ) {return 1;}
		@Override public void detect( GrayU8 input ) {detect = true;}
		@Override public boolean hasScale() {return true;}
		@Override public boolean hasOrientation() {	return true;}
		@Override public ImageType<GrayU8> getInputType() {return ImageType.SB_U8;}
		// @formatter:on
	}

	private static class MockConvert implements ConvertTupleDesc<TupleDesc_F64, TupleDesc_U8> {
		// @formatter:off
		@Override public TupleDesc_U8 createOutput() {return new TupleDesc_U8(1);}
		@Override public void convert( TupleDesc_F64 input, TupleDesc_U8 output ) {
			output.data[0] = (byte)input.data[0];
		}
		@Override public Class<TupleDesc_U8> getOutputType() {return TupleDesc_U8.class;}
		// @formatter:on
	}
}
