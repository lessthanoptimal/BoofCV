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

package boofcv.struct.feature;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.junit.After;
import org.junit.Test;

import java.io.*;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Tests which check to see if specific objects can be serialized or not
 *
 * WARNING: This will fail if the android module is loaded into memory at the same time.  It has a version of xmlstream
 * which isn't compatible and can't be excluded since Android is a massive jar.
 *
 * @author Peter Abeles
 */
public class TestObjectSerialization {

	Random rand = new Random(234);

	@After
	public void cleanup() {
		File f = new File("temp.txt");

		if( f.exists() )
			if( !f.delete() )
				throw new RuntimeException("Can't clean up temp.txt");
	}

	@Test
	public void testTupleDesc_F64() {
		TupleDesc_F64 orig = new TupleDesc_F64(20);
		for( int i = 0; i < orig.value.length; i++ ) {
			orig.value[i] = i;
		}

		TupleDesc_F64 found = serializeDeSerialize(orig);

		for( int i = 0; i < orig.value.length; i++ ) {
			assertEquals(orig.value[i],found.value[i],1e-8);
		}
	}

	@Test
	public void testSurfFeature() {
		BrightFeature orig = new BrightFeature(20);
		orig.white = true;
		for( int i = 0; i < orig.value.length; i++ ) {
			orig.value[i] = i;
		}

		BrightFeature found = serializeDeSerialize(orig);

		assertEquals(orig.white,found.white);
		for( int i = 0; i < orig.value.length; i++ ) {
			assertEquals(orig.value[i],found.value[i],1e-8);
		}
	}

	@Test
	public void testNccFeature() {
		NccFeature orig = new NccFeature(20);
		orig.mean = 1.2;
		orig.sigma = 3.4;
		for( int i = 0; i < orig.value.length; i++ ) {
			orig.value[i] = i;
		}

		NccFeature found = serializeDeSerialize(orig);

		assertEquals(orig.mean,found.mean,1e-8);
		assertEquals(orig.sigma,found.sigma,1e-8);
		for( int i = 0; i < orig.value.length; i++ ) {
			assertEquals(orig.value[i],found.value[i],1e-8);
		}
	}

	@Test
	public void testFastQueue() {
		FastQueue<GrayU8> list = new FastQueue<>(GrayU8.class,false);

		list.add(new GrayU8(1,2));
		list.add(new GrayU8(2,4));

		FastQueue<GrayU8> found = serializeDeSerialize(list);

		assertEquals(list.size(),found.size());
		assertTrue(list.type == found.type);
		assertFalse(found.isDeclareInstances());

		for( int i = 0; i < list.size; i++ ) {
			GrayU8 a = list.get(i);
			GrayU8 b = found.get(i);
			assertEquals(a.width,b.width);
			assertEquals(a.height,b.height);
			assertEquals(a.data.length,b.data.length);
		}
	}

	@Test
	public void testDMatrixRMaj() {
		DMatrixRMaj orig = new DMatrixRMaj(2,3,true,new double[]{1,2,3,4,5,6});

		DMatrixRMaj found = serializeDeSerialize(orig);

		assertEquals(orig.numRows, found.numRows);
		assertEquals(orig.numCols, found.numCols);

		for( int i = 0; i < orig.data.length; i++ ) {
			assertEquals(orig.data[i],found.data[i],1e-8);
		}
	}

	@Test
	public void testSingleBandImages() {
		Class []types = new Class[]{
					GrayU8.class, GrayS8.class,
					GrayU16.class,GrayS16.class,
					GrayS32.class,GrayS64.class,
					GrayF32.class,GrayF64.class};

		for( Class imageType : types ) {
			ImageGray original = GeneralizedImageOps.createSingleBand(imageType,3,5);
			GImageMiscOps.addUniform(original,rand,0,100);

			ImageGray found = serializeDeSerialize(original);

			assertEquals(original.width,found.width);
			assertEquals(original.height,found.height);
			assertEquals(original.stride,found.stride);
			assertEquals(original.startIndex,found.startIndex);

			for (int y = 0; y < original.height; y++) {
				for (int x = 0; x < original.width; x++) {
					double a = GeneralizedImageOps.get(original,x,y);
					double b = GeneralizedImageOps.get(found,x,y);

					assertTrue(a == b);
				}
			}
		}
	}

	@Test
	public void testPlanar() {
		Planar original = new Planar(GrayU8.class,40,50,3);
		GImageMiscOps.addUniform(original, rand, 0, 100);

		Planar found = serializeDeSerialize(original);

		assertEquals(original.width,found.width);
		assertEquals(original.height,found.height);
		assertEquals(original.stride,found.stride);
		assertEquals(original.startIndex, found.startIndex);
		assertEquals(original.getNumBands(),found.getNumBands());

		BoofTesting.assertEquals(original,found,1e-8);
	}

	@Test
	public void testIntrinsicParamters() {
		CameraPinholeRadial original = new CameraPinholeRadial().
				fsetK(1, 2, 3, 4, 5, 6, 7).fsetRadial(8,9).fsetTangental(10, 11);

		CameraPinholeRadial found = serializeDeSerialize(original);

		assertEquals(original.fx,found.fx,1e-8);
		assertEquals(original.fy, found.fy, 1e-8);
		assertEquals(original.skew,found.skew,1e-8);
		assertEquals(original.cx,found.cx,1e-8);
		assertEquals(original.cy,found.cy,1e-8);
		assertEquals(original.width,found.width);
		assertEquals(original.height,found.height);
		assertEquals(original.radial[0],found.radial[0],1e-8);
		assertEquals(original.radial[1],found.radial[1],1e-8);
		assertEquals(original.t1,found.t1,1e-8);
		assertEquals(original.t2,found.t2,1e-8);
	}

	private static <T>T serializeDeSerialize(Object orig ) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("temp.txt"));
			oos.writeObject(orig);
			oos.close();
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("temp.txt"));
			Object o = ois.readObject();
			ois.close();
			return (T)o;
		} catch( IOException | ClassNotFoundException e ) {
			e.printStackTrace();
			fail("Failed");
		}
		return null;
	}
}
