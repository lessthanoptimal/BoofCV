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

package boofcv.abst.feature.detect.extract;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class GeneralFeatureExtractorChecks {

	int width = 20;
	int height = 25;

	FeatureExtractor alg;

	ImageFloat32 image = new ImageFloat32(width,height);
	QueueCorner candidates = new QueueCorner(10);

	public abstract FeatureExtractor createAlg();

	public void testAll() {
		getUsesCandidates();
		setIgnoreBorder_basic();
		setIgnoreBorder_harder();
		setThreshold();
		setSearchRadius();
	}

	public void init() {
		alg = createAlg();

		alg.setIgnoreBorder(0);
		alg.setSearchRadius(1);
		alg.setThreshold(5);

		ImageMiscOps.fill(image,0);
		if( alg.getUsesCandidates() )
			candidates.reset();
		else
			candidates = null;
	}

	@Test
	public void getUsesCandidates() {
		init();

		if( !alg.getUsesCandidates() )
			return;

		setPixel(2, 4, 10); // maximum
		image.set(2,3,9); // not a maximum

		// with the candidate list only (2,4) should be found
		QueueCorner found = new QueueCorner(10);
		alg.process(image,null,candidates,null,found);
		assertEquals(1, found.size);
		assertEquals(2,found.get(0).x);
		assertEquals(4,found.get(0).y);

		// remove (2,4) from the list, none should be found
		candidates.removeTail();
		found.reset();
		alg.process(image,null,candidates,null, found);
		assertEquals(0, found.size);

		// since it got two different answers that were dependent on the candidate list it passes the test
	}

	@Test
	public void setIgnoreBorder_basic() {
		init();

		int r = alg.canDetectBorder() ? 0 : alg.getSearchRadius();

		// place features inside the image at the border of where they can be processed
		setPixel(r,r,10);
		setPixel(width-r-1,height-r-1,10);

		// should find both of them
		QueueCorner found = new QueueCorner(10);
		alg.process(image,null,candidates,null,found);
		assertEquals(2, found.size);

		// now it shouldn't find them
		found.reset();
		alg.setIgnoreBorder(1);
		alg.process(image,null,candidates,null, found);
		assertEquals(0, found.size);
	}

	@Test
	public void setIgnoreBorder_harder() {
		init();

		int r = alg.canDetectBorder() ? 1 : alg.getSearchRadius()+1;

		// place features inside the image at the border of where they can be processed
		setPixel(r,r,10);
		setPixel(width-r-1,height-r-1,10);
		// then even more intense features right next to them outside of where they should be processed
		setPixel(r-1,r,15);
		setPixel(r,r-1,15);
		setPixel(width-r,height-r-1,15);
		setPixel(width-r-1,height-r,15);

		// If it doesn't find both of those features then its actually looking outside the ignore border
		QueueCorner found = new QueueCorner(10);
		alg.setIgnoreBorder(1);
		alg.process(image,null,candidates,null,found);
		assertEquals(2, found.size);

	}

	@Test
	public void setThreshold() {
		init();

		setPixel(5,6,10);
		QueueCorner found = new QueueCorner(10);
		alg.process(image,null,candidates,null,found);
		assertEquals(1, found.size);

		// shouldn't find it after the threshold is set above its values
		alg.setThreshold(20);
		found.reset();
		alg.process(image,null,candidates,null,found);
		assertEquals(0, found.size);
	}

	@Test
	public void setSearchRadius() {
		init();

		setPixel(5, 6, 9);
		setPixel(7,6,10);

		// should find them both on the first pass
		QueueCorner found = new QueueCorner(10);
		alg.setSearchRadius(1);
		alg.process(image,null,candidates,null,found);
		assertEquals(2, found.size);

		// only one of the aftear the search radius is expanded
		alg.setSearchRadius(2);
		found.reset();
		alg.process(image,null,candidates,null,found);
		assertEquals(1, found.size);
	}

	@Test
	public void canDetectBorder() {
		// place two features along the image border
		setPixel(0,0,10);
		setPixel(width-1,height-1,10);

		// the solution should depend on its capabilities
		QueueCorner found = new QueueCorner(10);
		alg.process(image,null,candidates,null,found);
		if( found.size == 2 )
			assertTrue(alg.canDetectBorder());
		else if( found.size == 0 )
			assertTrue(!alg.canDetectBorder());
		else
			fail("Unexpected number of found");
	}

	public void setPixel( int x , int y , float intensity ) {
		image.set(x,y,intensity);
		if( alg.getUsesCandidates() )
			candidates.add(x,y);
	}
}
