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

package boofcv.struct.image;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestPlanar extends BoofStandardJUnit {

	int imgWidth = 10;
	int imgHeight = 20;

	@Test void constructor() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,imgWidth, imgHeight, 3);

		assertSame(GrayU8.class, img.getBandType());
		assertEquals(3, img.bands.length);
		assertEquals(3, img.getNumBands());
		assertEquals(imgWidth, img.width);
		assertEquals(imgHeight, img.height);
		for (int i = 0; i < 3; i++) {
			assertNotNull(img.bands[i]);
		}
	}

	@Test void getBand() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,imgWidth, imgHeight, 3);

		assertNotNull(img.getBand(0));

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

	@Test void subimage() {
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

	@Test void reshape_wh() {
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

	@Test void reshape_whb() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,5, 10, 3);

		// reshape to something smaller
		img.reshape(5,4, 2);
		assertEquals(5,img.getWidth());
		assertEquals(4,img.getHeight());
		assertEquals(5,img.getBand(0).getWidth());
		assertEquals(4,img.getBand(0).getHeight());
		assertEquals(2,img.getNumBands());
		assertEquals(2,img.getImageType().numBands);

		// number of bands constant
		img.reshape(5,5, 2);
		assertEquals(5, img.getWidth());
		assertEquals(5, img.getHeight());
		assertEquals(5, img.getBand(0).getWidth());
		assertEquals(5, img.getBand(0).getHeight());
		assertEquals(2, img.getNumBands());
		assertEquals(2,img.getImageType().numBands);

		// reshape to something larger
		img.reshape(15, 21);
		assertEquals(15, img.getWidth());
		assertEquals(21, img.getHeight());
		assertEquals(15, img.getBand(0).getWidth());
		assertEquals(21, img.getBand(0).getHeight());
		assertEquals(2,  img.getNumBands());
		assertEquals(2,img.getImageType().numBands);

		// increase number of bands
		img.reshape(15, 21,3);
		assertEquals(15, img.getWidth());
		assertEquals(21, img.getHeight());
		assertEquals(15, img.getBand(0).getWidth());
		assertEquals(21, img.getBand(0).getHeight());
		assertEquals(3,  img.getNumBands());
		assertEquals(3,img.getImageType().numBands);

	}

	@Test void reshape_subimage() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,5, 10, 3);
		img = img.subimage(0,0,2,2, null);

		try {
			img.reshape(10,20);
			fail("Should have thrown exception");
		} catch( IllegalArgumentException ignore ) {}
	}

	@Test void setTo() {
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
	@Test void setTo_mismatch() {
		Planar<GrayU8> a = new Planar<>(GrayU8.class,5, 10, 3);
		Planar<GrayU8> b = new Planar<>(GrayU8.class,6, 11, 3);

		a.setTo(b);

		assertEquals(a.width, 6);
		assertEquals(b.height, 11);
	}

	@Test void serialize() throws IOException, ClassNotFoundException {

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

	@Test void reorderBands() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,5, 10, 3);

		GrayU8 band0 = img.getBand(0);
		GrayU8 band1 = img.getBand(1);
		GrayU8 band2 = img.getBand(2);

		img.reorderBands(2,0,1);

		assertSame(band0, img.getBand(1));
		assertSame(band1, img.getBand(2));
		assertSame(band2, img.getBand(0));
	}

	@Test void setNumberOfBands() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,5, 10, 2);

		assertEquals(2, img.getNumBands());
		assertEquals(2, img.getImageType().numBands);

		img.setNumberOfBands(3);
		assertEquals(3, img.getNumBands());
		assertEquals(3, img.getImageType().numBands);
		for (int i = 0; i < img.getNumBands(); i++) {
			assertEquals(5, img.getBand(i).width);
			assertEquals(10, img.getBand(i).height);
		}

		img.setNumberOfBands(1);
		assertEquals(1, img.getNumBands());
		assertEquals(1, img.getImageType().numBands);
		for (int i = 0; i < img.getNumBands(); i++) {
			assertEquals(5, img.getBand(i).width);
			assertEquals(10, img.getBand(i).height);
		}
	}

	@Test void get24u8() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,2,3,3);
		img.getBand(0).set(0,1,233);
		img.getBand(1).set(0,1,16);
		img.getBand(2).set(0,1,128);
		img.getBand(0).set(1,1,16);
		img.getBand(1).set(1,1,0);
		img.getBand(2).set(1,1,200);

		int expected0 = (233<<16) | (16<<8) | (128);
		int expected1 = (16<<16)  |           (200);

		assertEquals(expected0,img.get24u8(0,1));
		assertEquals(expected1,img.get24u8(1,1));
	}

	@Test void get32u8() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,2,3,4);
		img.getBand(0).set(0,1,208);
		img.getBand(1).set(0,1,233);
		img.getBand(2).set(0,1,16);
		img.getBand(3).set(0,1,128);

		img.getBand(0).set(1,1,217);
		img.getBand(1).set(1,1,16);
		img.getBand(2).set(1,1,0);
		img.getBand(3).set(1,1,200);

		int expected0 = (208<<24) | (233<<16) | (16<<8) | (128);
		int expected1 = (217<<24) | (16<<16)  |           (200);

		assertEquals(expected0,img.get32u8(0,1));
		assertEquals(expected1,img.get32u8(1,1));
	}

	@Test void set32u8() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,2,3,5);

		int expected0 = (208<<24) | (233<<16) | (16<<8) | (128);
		int expected1 = (217<<24) | (16<<16)  |           (200);

		img.set32u8(0,1,expected0);
		img.set32u8(1,1,expected1);

		assertEquals(208,img.getBand(0).get(0,1));
		assertEquals(233,img.getBand(1).get(0,1));
		assertEquals(16,img.getBand(2).get(0,1));
		assertEquals(128,img.getBand(3).get(0,1));


		assertEquals(217,img.getBand(0).get(1,1));
		assertEquals(16,img.getBand(1).get(1,1));
		assertEquals(0,img.getBand(2).get(1,1));
		assertEquals(200,img.getBand(3).get(1,1));
	}

	@Test void set24u8() {
		Planar<GrayU8> img = new Planar<>(GrayU8.class,2,3,3);

		int expected0 = (233<<16) | (16<<8) | (128);
		int expected1 = (16<<16)  |           (200);

		img.set24u8(0,1,expected0);
		img.set24u8(1,1,expected1);

		assertEquals(233,img.getBand(0).get(0,1));
		assertEquals(16,img.getBand(1).get(0,1));
		assertEquals(128,img.getBand(2).get(0,1));


		assertEquals(16,img.getBand(0).get(1,1));
		assertEquals(0,img.getBand(1).get(1,1));
		assertEquals(200,img.getBand(2).get(1,1));
	}
}
