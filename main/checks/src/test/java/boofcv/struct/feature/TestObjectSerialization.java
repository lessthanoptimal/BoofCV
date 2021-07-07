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

package boofcv.struct.feature;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.*;
import org.ddogleg.struct.FastArray;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests which check to see if specific objects can be serialized or not
 *
 * WARNING: This will fail if the android module is loaded into memory at the same time. It has a version of xml 
 * stream which isn't compatible and can't be excluded since Android is a massive jar.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class TestObjectSerialization {

	Random rand = new Random(234);

	@AfterEach
	public void cleanup() {
		File f = new File("temp.txt");

		if( f.exists() )
			if( !f.delete() )
				throw new RuntimeException("Can't clean up temp.txt");
	}

	@Test
	void testTupleDesc_F64() {
		TupleDesc_F64 orig = new TupleDesc_F64(20);
		for(int i = 0; i < orig.data.length; i++ ) {
			orig.data[i] = i;
		}

		TupleDesc_F64 found = serializeDeSerialize(orig);
		assertNotNull(found);

		for(int i = 0; i < orig.data.length; i++ ) {
			assertEquals(orig.data[i],found.data[i],1e-8);
		}
	}

	@Test
	void testNccFeature() {
		NccFeature orig = new NccFeature(20);
		orig.mean = 1.2;
		orig.sigma = 3.4;
		for(int i = 0; i < orig.data.length; i++ ) {
			orig.data[i] = i;
		}

		NccFeature found = serializeDeSerialize(orig);

		assertNotNull(found);
		assertEquals(orig.mean,found.mean,1e-8);
		assertEquals(orig.sigma,found.sigma,1e-8);
		for(int i = 0; i < orig.data.length; i++ ) {
			assertEquals(orig.data[i],found.data[i],1e-8);
		}
	}

	@Test
	void serialize_FastArray() {
		FastArray<GrayU8> list = new FastArray<>(GrayU8.class);

		list.add(new GrayU8(1,2));
		list.add(new GrayU8(2,4));

		FastArray<GrayU8> found = serializeDeSerialize(list);

		assertNotNull(found);
		assertEquals(list.size(),found.size());
		assertSame(list.type, found.type);

		for( int i = 0; i < list.size; i++ ) {
			GrayU8 a = list.get(i);
			GrayU8 b = found.get(i);
			assertEquals(a.width,b.width);
			assertEquals(a.height,b.height);
			assertEquals(a.data.length,b.data.length);
		}
	}

	@Test
	void testDMatrixRMaj() {
		DMatrixRMaj orig = new DMatrixRMaj(2,3,true, 1,2,3,4,5,6);

		DMatrixRMaj found = serializeDeSerialize(orig);

		assertNotNull(found);
		assertEquals(orig.numRows, found.numRows);
		assertEquals(orig.numCols, found.numCols);

		for( int i = 0; i < orig.data.length; i++ ) {
			assertEquals(orig.data[i],found.data[i],1e-8);
		}
	}

	@Test
	void testSingleBandImages() {
		Class []types = new Class[]{
					GrayU8.class, GrayS8.class,
					GrayU16.class,GrayS16.class,
					GrayS32.class,GrayS64.class,
					GrayF32.class,GrayF64.class};

		for( Class imageType : types ) {
			ImageGray original = GeneralizedImageOps.createSingleBand(imageType,3,5);
			GImageMiscOps.addUniform(original,rand,0,100);

			ImageGray found = serializeDeSerialize(original);

			assertNotNull(found);
			assertEquals(original.width,found.width);
			assertEquals(original.height,found.height);
			assertEquals(original.stride,found.stride);
			assertEquals(original.startIndex,found.startIndex);

			for (int y = 0; y < original.height; y++) {
				for (int x = 0; x < original.width; x++) {
					double a = GeneralizedImageOps.get(original,x,y);
					double b = GeneralizedImageOps.get(found,x,y);

					assertEquals(a, b);
				}
			}
		}
	}

	@Test
	void testPlanar() {
		Planar original = new Planar(GrayU8.class,40,50,3);
		GImageMiscOps.addUniform(original, rand, 0, 100);

		Planar found = serializeDeSerialize(original);

		assertNotNull(found);
		assertEquals(original.width,found.width);
		assertEquals(original.height,found.height);
		assertEquals(original.stride,found.stride);
		assertEquals(original.startIndex, found.startIndex);
		assertEquals(original.getNumBands(),found.getNumBands());

		BoofTesting.assertEquals(original,found,1e-8);
	}

	@Test
	void tebstIntrinsicParamters() {
		CameraPinholeBrown original = new CameraPinholeBrown().
				fsetK(1, 2, 3, 4, 5, 6, 7).fsetRadial(8,9).fsetTangental(10, 11);

		CameraPinholeBrown found = serializeDeSerialize(original);

		assertNotNull(found);
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
