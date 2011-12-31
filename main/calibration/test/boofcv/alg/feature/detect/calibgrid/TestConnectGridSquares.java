/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.feature.detect.calibgrid.TestExtractOrderedTargetPoints.createBlob;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestConnectGridSquares {

	@Test
	public void stuff() {
		fail("implement");
	}

	@Test
	public void basic() throws InvalidTarget {
		List<SquareBlob> blobs = new ArrayList<SquareBlob>();

		blobs.add( createBlob(5,5,1));
		blobs.add( createBlob(7,5,1));
		blobs.add( createBlob(9,5,1));
		blobs.add( createBlob(5,2,1));
		blobs.add( createBlob(7,2,1));
		blobs.add( createBlob(9,2,1));

		ConnectGridSquares.connect(blobs);

		// see if they have the expected number of connections
		assertEquals(2, blobs.get(0).conn.size());
		assertEquals(3, blobs.get(1).conn.size());
		assertEquals(2, blobs.get(2).conn.size());
		assertEquals(2, blobs.get(3).conn.size());
		assertEquals(3, blobs.get(4).conn.size());
		assertEquals(2, blobs.get(5).conn.size());

		// sanity check one of the nodes specific connections
		assertTrue(blobs.get(2).conn.contains(blobs.get(1)));
		assertTrue(blobs.get(2).conn.contains(blobs.get(5)));
	}

}
