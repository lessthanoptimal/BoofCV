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

package boofcv.struct.image;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.io.*;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPlanar {

	Random rand = new Random(234);
	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void constructor() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,imgWidth, imgHeight, 3);

		assertTrue(GrayU8.class == img.getBandType());
		assertTrue(3 == img.bands.length);
		assertTrue(3 == img.getNumBands());
		assertTrue(imgWidth == img.width);
		assertTrue(imgHeight == img.height);
		for (int i = 0; i < 3; i++) {
			assertTrue(img.bands[i] != null);
		}
	}

	@Test
	public void getBand() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,imgWidth, imgHeight, 3);

		assertTrue(img.getBand(0) != null);

		try {
			img.getBand(-1);
			fail("Exception should have been thrown");
		} catch (IllegalArgumentException e) {

		}
		try {
			img.getBand(3);
			fail("Exception should have been thrown");
		} catch (IllegalArgumentException e) {

		}
	}

	@Test
	public void subimage() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,5, 10, 3);
		assertFalse(img.isSubimage());

		Planar<GrayU8> sub = img.subimage(2,3,4,6, null);
		
		assertEquals(3,sub.getNumBands());
		assertEquals(2,sub.getWidth());
		assertEquals(3,sub.getHeight());
		assertTrue(sub.isSubimage());

		for( int i = 0; i < sub.getNumBands(); i++ )
			assertEquals(img.getBand(i).get(2,3),sub.getBand(i).get(0,0));
	}

	@Test
	public void reshape() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,5, 10, 3);

		// reshape to something smaller
		img.reshape(5,4);
		assertEquals(5, img.getWidth());
		assertEquals(4,img.getHeight());

		// reshape to something larger
		img.reshape(15, 21);
		assertEquals(15,img.getWidth());
		assertEquals(21, img.getHeight());
	}

	@Test
	public void reshape_subimage() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,5, 10, 3);
		img = img.subimage(0,0,2,2, null);

		try {
			img.reshape(10,20);
			fail("Should have thrown exception");
		} catch( IllegalArgumentException ignore ) {}
	}

	@Test
	public void setTo() {
		Planar<GrayU8> a = new Planar<>(GrayU8.class,5, 10, 3);
		a.getBand(0).set(1,2,3);
		a.getBand(1).set(2,1,4);
		a.getBand(2).set(2,2,5);

		Planar<GrayU8> b = new Planar<>(GrayU8.class,5, 10, 3);
		b.setTo(a);

		BoofTesting.assertEquals(a,b,1e-8);

		// try a sub-image now
		Planar<GrayU8> c = new Planar<>(GrayU8.class,20, 20, 3);
		c = c.subimage(7,8,12,18, null);
		c.setTo(a);

		BoofTesting.assertEquals(a, c, 1e-8);
	}

	/**
	 * The two matrices do not have the same shape
	 */
	@Test
	public void setTo_mismatch() {
		Planar<GrayU8> a = new Planar<>(GrayU8.class,5, 10, 3);
		Planar<GrayU8> b = new Planar<>(GrayU8.class,6, 11, 3);

		a.setTo(b);

		assertEquals(a.width, 6);
		assertEquals(b.height, 11);
	}

	@Test
	public void serialize() throws IOException, ClassNotFoundException {

		// randomly fill the image
		Planar<GrayU8> imgA = new Planar<>(GrayU8.class,5, 10, 3);
		GImageMiscOps.fillUniform(imgA, rand, -10, 10);

		// make a copy of the original
		Planar<GrayU8> imgB = imgA.clone();


		ByteArrayOutputStream streamOut = new ByteArrayOutputStream(1000);
		ObjectOutputStream out = new ObjectOutputStream(streamOut);
		out.writeObject(imgA);
		out.close();


		ByteArrayInputStream streamIn = new ByteArrayInputStream(streamOut.toByteArray());
		ObjectInputStream in = new ObjectInputStream(streamIn);

		Planar<GrayU8> found = (Planar)in.readObject();

		// see if everything is equals
		BoofTesting.assertEquals(imgA, imgB, 1e-8);
		BoofTesting.assertEquals(imgA, found, 1e-8);
	}

	@Test
	public void reorderBands() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,5, 10, 3);

		GrayU8 band0 = img.getBand(0);
		GrayU8 band1 = img.getBand(1);
		GrayU8 band2 = img.getBand(2);

		img.reorderBands(2,0,1);

		assertTrue( band0 == img.getBand(1));
		assertTrue( band1 == img.getBand(2));
		assertTrue( band2 == img.getBand(0));
	}
}
