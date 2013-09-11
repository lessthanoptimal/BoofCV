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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociateEuclidean_F64;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestAssociateSurfBasic {

	AssociateSurfBasic alg = createAlg();

	/**
	 * Two features with different laplacian signs should never be associated
	 */
	@Test
	public void checkAssociateByIntensity() {
		FastQueue<SurfFeature> src = new FastQueue<SurfFeature>(10,SurfFeature.class,false);
		FastQueue<SurfFeature> dst = new FastQueue<SurfFeature>(10,SurfFeature.class,false);

		src.add( createDesc(true,10));
		dst.add( createDesc(true,0));
		dst.add( createDesc(false,10));

		alg.setSrc(src);
		alg.setDst(dst);
		alg.associate();
		FastQueue<AssociatedIndex> matches = alg.getMatches();

		assertEquals(1,matches.size());
		// it should match with the first one, even though the second has a better feature set
		assertEquals(0,matches.get(0).dst);
	}

	@Test
	public void basicAssociation() {
		FastQueue<SurfFeature> src = new FastQueue<SurfFeature>(10,SurfFeature.class,false);
		FastQueue<SurfFeature> dst = new FastQueue<SurfFeature>(10,SurfFeature.class,false);

		// create a list where some should be matched and others not
		src.add( createDesc(true,10));
		src.add( createDesc(true,12));
		src.add( createDesc(false,5));
		src.add( createDesc(false,2344));
		src.add( createDesc(false,1000));
		dst.add( createDesc(true,0));
		dst.add( createDesc(true,10.1));
		dst.add( createDesc(true,13));
		dst.add( createDesc(false,0.1));
		dst.add( createDesc(false,7));

		alg.setSrc(src);
		alg.setDst(dst);
		alg.associate();
		FastQueue<AssociatedIndex> matches = alg.getMatches();

		assertEquals(3,matches.size());
		assertTrue(matches.get(0).fitScore != 0);
		assertEquals(0,matches.get(0).src);
		assertEquals(1,matches.get(0).dst);
		assertTrue(matches.get(1).fitScore != 0);
		assertEquals(1,matches.get(1).src);
		assertEquals(2,matches.get(1).dst);
		assertTrue(matches.get(2).fitScore != 0);
		assertEquals(2,matches.get(2).src);
		assertEquals(4,matches.get(2).dst);

		// see if the expected number of features are in the unassociated list
		GrowQueue_I32 unassoc = alg.unassociatedSrc;
		assertEquals(2,unassoc.size);

		// make sure none of the unassociated are contained in the associated list
		for( int i = 0; i < unassoc.size; i++ ) {
			int index = unassoc.data[i];
			for( int j = 0; j < matches.size(); j++ ) {
				if( matches.get(j).src == index )
					fail("match found");
			}
		}
	}

	private AssociateSurfBasic createAlg() {

		ScoreAssociation<TupleDesc_F64> score = new ScoreAssociateEuclidean_F64();
		AssociateDescription<TupleDesc_F64> assoc = FactoryAssociation.greedy(score, 20, true);

		return new AssociateSurfBasic(assoc);
	}

	private SurfFeature createDesc( boolean laplace , double value ) {
		SurfFeature ret = new SurfFeature(64);

		ret.laplacianPositive = laplace;
		ret.value[0] = value;

		return ret;
	}

	@Test
	public void checkUnassociated() {
		FastQueue<SurfFeature> src = new FastQueue<SurfFeature>(10,SurfFeature.class,false);
		FastQueue<SurfFeature> dst = new FastQueue<SurfFeature>(10,SurfFeature.class,false);

		src.add( createDesc(true,10));
		src.add( createDesc(true,12));
		src.add( createDesc(false,5));
		dst.add( createDesc(true,0));
		dst.add( createDesc(true,10.1));
		dst.add( createDesc(true,13));
		dst.add( createDesc(false,0.1));
		dst.add( createDesc(false,7));

		ScoreAssociation<TupleDesc_F64> score = new ScoreAssociateEuclidean_F64();
		AssociateDescription<TupleDesc_F64> assoc = FactoryAssociation.greedy(score, 20, true);
		AssociateSurfBasic alg = new AssociateSurfBasic(assoc);

		alg.setSrc(src);
		alg.setDst(dst);
		alg.associate();
		FastQueue<AssociatedIndex> matches = alg.getMatches();
	}
}
