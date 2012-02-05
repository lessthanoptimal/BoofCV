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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSegmentGaussianEM {
	
	Random rand = new Random(234234);
	
	@Test
	public void simple() {
		// create a set of data from two gaussian distribution
		List<Double> list = new ArrayList<Double>();
		
		double mean0=2;
		double sigma0=1;
		double mean1=20;
		double sigma1=3;
		
		for( int i = 0; i < 100; i++ ) {
			list.add( mean0+rand.nextGaussian()*sigma0 );
			list.add( mean1+rand.nextGaussian()*sigma1 );
		}
		Collections.shuffle(list,rand);
		

		// extract the original distributions
		SegmentGaussianEM alg = new SegmentGaussianEM(list.size(),30,1e-5);

		alg.reset();
		for( int i = 0; i < list.size(); i++ ) {
			alg.addValue(list.get(i));
		}

		alg.process();

		assertEquals(mean0,alg.getMean0(),0.3);
		assertEquals(sigma0*sigma0,alg.getVariance0(),1);
		assertEquals(mean1,alg.getMean1(),0.3);
		assertEquals(sigma1*sigma1,alg.getVariance1(),1);
	}
}
