/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.io;

import boofcv.struct.image.ImageUInt8;
import org.ddogleg.struct.FastQueue;
import org.junit.After;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests which check to see if specific objects can be serialized or not
 *
 * @author Peter Abeles
 */
public class TestObjectSerialization {

	@After
	public void cleanup() {
		File f = new File("temp.txt");

		if( f.exists() )
			if( !f.delete() )
				throw new RuntimeException("Can't clean up temp.txt");
	}

	// No idea where else to but a test for FastQueue
	@Test
	public void testFastQueue() {
		FastQueue<ImageUInt8> list = new FastQueue<ImageUInt8>(ImageUInt8.class,false);

		list.add(new ImageUInt8(1,2));
		list.add(new ImageUInt8(2,4));

		UtilIO.saveXML(list, "temp.txt");

		FastQueue<ImageUInt8> found = UtilIO.loadXML("temp.txt");

		assertEquals(list.size(),found.size());
		assertTrue(list.type==found.type);

		for( int i = 0; i < list.size; i++ ) {
			ImageUInt8 a = list.get(i);
			ImageUInt8 b = found.get(i);
			assertEquals(a.width,b.width);
			assertEquals(a.height,b.height);
			assertEquals(a.data.length,b.data.length);
		}
	}
}
