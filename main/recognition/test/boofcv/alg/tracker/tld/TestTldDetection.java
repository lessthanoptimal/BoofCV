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

package boofcv.alg.tracker.tld;

import boofcv.struct.ImageRectangle;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTldDetection {

	@Test
	public void computeTemplateConfidence() {
		TldDetection<GrayU8> alg = new TldDetection<>();
		alg.config = new TldParameters();
		alg.config.confidenceThresholdUpper = 0.6;
		alg.template = new HelperTemplate();

		for( int i = 0; i < 4; i++ ) {
			alg.fernRegions.add(new ImageRectangle(i,i,i,i));
		}

		alg.computeTemplateConfidence();

		assertEquals(3,alg.candidateDetections.size());

		for( int i = 0; i < 3; i++ ) {
			TldRegion r = alg.candidateDetections.get(i);
			assertEquals(0,r.connections);
			assertTrue(r.confidence>0.6);
			assertTrue(r.rect.x0 != 0);
		}
	}

	/**
	 * See if regions with larger N than P are filtered out
	 */
	@Test
	public void selectBestRegionsFern_largerN() {
		TldDetection<GrayU8> alg = new TldDetection<>();
		alg.config = new TldParameters();
		alg.config.maximumCascadeConsider = 20;

		// check eliminate ones which are more N than P
		for( int i = 0; i < 10; i++ ) {
			alg.fernInfo.grow();
			alg.fernInfo.get(i).sumP = 6;
			alg.fernInfo.get(i).sumN = 6;
		}

		// larger max N will make its likelihood smaller
		alg.selectBestRegionsFern(300,200);

		assertEquals(0,alg.fernRegions.size());
	}

	/**
	 * See if the case where there are fewer than the maximum number of regions is handled correctly
	 */
	@Test
	public void selectBestRegionsFern_smaller() {
		TldDetection<GrayU8> alg = new TldDetection<>();
		alg.config = new TldParameters();
		alg.config.maximumCascadeConsider = 20;

		// all 10 should be accepted
		for( int i = 0; i < 10; i++ ) {
			alg.fernInfo.grow();
			alg.fernInfo.get(i).r = new ImageRectangle(i,i,i,i);
			alg.fernInfo.get(i).sumP = 20;
			alg.fernInfo.get(i).sumN = 6;
		}
		alg.selectBestRegionsFern(200,200);

		assertEquals(10,alg.fernRegions.size());
	}

	/**
	 * See if the case where there are more than the maximum number of regions is handled correctly
	 */
	@Test
	public void selectBestRegionsFern_larger() {
		TldDetection<GrayU8> alg = new TldDetection<>();
		alg.config = new TldParameters();
		alg.config.maximumCascadeConsider = 20;

		// all 10 should be accepted
		for( int i = 0; i < 30; i++ ) {
			alg.fernInfo.grow();
			alg.fernInfo.get(i).r = new ImageRectangle(i,i,i,i);
			alg.fernInfo.get(i).sumP = 50-i;
			alg.fernInfo.get(i).sumN = 6;
		}
		alg.selectBestRegionsFern(200,200);

		assertEquals(20,alg.fernRegions.size());
		// should contain all the best ones
		for( int i = 0; i < 20; i++ ) {
			assertTrue(alg.fernRegions.contains(alg.fernInfo.get(i).r));
		}
	}

	protected static class HelperTemplate extends TldTemplateMatching {

		int numCalled = 0;

		@Override
		public double computeConfidence( ImageRectangle r ) {
			return 0.55 + (numCalled++)*0.1;
		}
	}

}
