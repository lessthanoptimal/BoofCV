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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.feature.detect.quadblob.QuadBlob;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestOrderChessboardQuadBlobs {

	Random rand = new Random(234);

	/**
	 * Give it many different graphs and see if it can reconstruct the order correctly
	 */
	@Test
	public void exhaustiveValid() {
		for( int numRows = 2; numRows <= 10; numRows++ ) {
			for( int numCols = 2; numCols <= 10; numCols++ ) {
				List<QuadBlob> expected = createBlobs(numCols,numRows);

				// randomize the order
				List<QuadBlob> input = new ArrayList<QuadBlob>();
				input.addAll(expected);
				Collections.shuffle(input,rand);

				for( QuadBlob b : input ) {
					Collections.shuffle(b.conn,rand);
				}

				OrderChessboardQuadBlobs alg = new OrderChessboardQuadBlobs(numCols,numRows);
				assertTrue(numCols+" "+numRows,alg.order(input));

				// check the node order to see if it's as expected
				List<QuadBlob> found = alg.getResults();
				assertEquals(expected.size(),found.size());
				for( int i = 0; i < input.size(); i++ ) {
					assertTrue(expected.get(i) == found.get(i));
				}
			}
		}
	}

	public List<QuadBlob> createBlobs( int numCols , int numRows ) {
		List<QuadBlob> large = new ArrayList<QuadBlob>();

		int side = 20;

		int largeCols = numCols/2 + numCols%2;
		int largeRows = numRows/2 + numRows%2;

		for( int i = 0; i < largeRows; i++ ) {
			for( int j = 0; j < largeCols; j++ ) {
				QuadBlob b = new QuadBlob();
				b.center = new Point2D_I32();

				b.center.x = 2*j*side;
				b.center.y = 2*i*side;

				large.add(b);
			}
		}

		List<QuadBlob> small = new ArrayList<QuadBlob>();

		int smallCols = numCols/2;
		int smallRows = numRows/2;

		for( int i = 0; i < smallRows; i++ ) {
			for( int j = 0; j < smallCols; j++ ) {
				QuadBlob b = new QuadBlob();
				b.center = new Point2D_I32();

				b.center.x = 2*j*side+side;
				b.center.y = 2*i*side+side;

				small.add(b);

				int indexLarge = i*largeCols + j;

				connect(b,large.get(indexLarge));
				if( j+1 < largeCols)
					connect(b, large.get(indexLarge+1));

				if( i+1 < largeRows) {
					indexLarge = (i+1)*largeCols + j;
					connect(b, large.get(indexLarge));
					if( j+1 < largeCols)
						connect(b, large.get(indexLarge+1));
				}
			}
		}

		List<QuadBlob> all = new ArrayList<QuadBlob>();

		for( int i = 0; i < largeRows; i++ ) {
			for( int j = 0; j < largeCols; j++ ) {
				all.add(large.get(i*largeCols+j));
			}
			if( i < smallRows ) {
				for( int j = 0; j < smallCols; j++ ) {
					all.add(small.get(i*smallCols+j));
				}
			}
		}

		// simple sanity check on truth
		assertEquals(smallRows*smallCols + largeRows*largeCols,all.size());

		return all;
	}

	private void connect( QuadBlob a , QuadBlob b ) {
		a.conn.add( b );
		b.conn.add( a );
	}

}
