/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.testing.BoofTesting;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestMultiSpectral {

	Random rand = new Random(234);
	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void constructor() {
		MultiSpectral<ImageUInt8> img = new MultiSpectral<ImageUInt8>(ImageUInt8.class,imgWidth, imgHeight, 3);

		assertTrue(ImageUInt8.class == img.getType());
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
		MultiSpectral<ImageUInt8> img = new MultiSpectral<ImageUInt8>(ImageUInt8.class,imgWidth, imgHeight, 3);

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
		MultiSpectral<ImageUInt8> img = new MultiSpectral<ImageUInt8>(ImageUInt8.class,5, 10, 3);
		assertFalse(img.isSubimage());

		MultiSpectral<ImageUInt8> sub = img.subimage(2,3,4,6, null);
		
		assertEquals(3,sub.getNumBands());
		assertEquals(2,sub.getWidth());
		assertEquals(3,sub.getHeight());
		assertTrue(sub.isSubimage());

		for( int i = 0; i < sub.getNumBands(); i++ )
			assertEquals(img.getBand(i).get(2,3),sub.getBand(i).get(0,0));
	}

	@Test
	public void reshape() {
		MultiSpectral<ImageUInt8> img = new MultiSpectral<ImageUInt8>(ImageUInt8.class,5, 10, 3);

		// reshape to something smaller
		img.reshape(5,4);
		assertEquals(5,img.getWidth());
		assertEquals(4,img.getHeight());

		// reshape to something larger
		img.reshape(15,21);
		assertEquals(15,img.getWidth());
		assertEquals(21,img.getHeight());
	}

	@Test
	public void reshape_subimage() {
		MultiSpectral<ImageUInt8> img = new MultiSpectral<ImageUInt8>(ImageUInt8.class,5, 10, 3);
		img = img.subimage(0,0,2,2, null);

		try {
			img.reshape(10,20);
			fail("Should have thrown exception");
		} catch( IllegalArgumentException ignore ) {}
	}

	@Test
	public void setTo() {
		MultiSpectral<ImageUInt8> a = new MultiSpectral<ImageUInt8>(ImageUInt8.class,5, 10, 3);
		a.getBand(0).set(1,2,3);
		a.getBand(1).set(2,1,4);
		a.getBand(2).set(2,2,5);

		MultiSpectral<ImageUInt8> b = new MultiSpectral<ImageUInt8>(ImageUInt8.class,5, 10, 3);
		b.setTo(a);

		BoofTesting.assertEquals(a,b,1e-8);

		// try a sub-image now
		MultiSpectral<ImageUInt8> c = new MultiSpectral<ImageUInt8>(ImageUInt8.class,20, 20, 3);
		c = c.subimage(7,8,12,18, null);
		c.setTo(a);

		BoofTesting.assertEquals(a,c,1e-8);
	}

	@Test
	public void serialize() throws IOException, ClassNotFoundException {

		// randomly fill the image
		MultiSpectral<ImageUInt8> imgA = new MultiSpectral<ImageUInt8>(ImageUInt8.class,5, 10, 3);
		GImageMiscOps.fillUniform(imgA, rand, -10, 10);

		// make a copy of the original
		MultiSpectral<ImageUInt8> imgB = imgA.clone();


		ByteOutputStream streamOut = new ByteOutputStream(1000);
		ObjectOutputStream out = new ObjectOutputStream(streamOut);
		out.writeObject(imgA);
		out.close();


		ByteInputStream streamIn = new ByteInputStream(streamOut.getBytes(),streamOut.getCount());
		ObjectInputStream in = new ObjectInputStream(streamIn);

		MultiSpectral<ImageUInt8> found = (MultiSpectral)in.readObject();

		// see if everything is equals
		BoofTesting.assertEquals(imgA, imgB, 1e-8);
		BoofTesting.assertEquals(imgA, found, 1e-8);
	}
}
