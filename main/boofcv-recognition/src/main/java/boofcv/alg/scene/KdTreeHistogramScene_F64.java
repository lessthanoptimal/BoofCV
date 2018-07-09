/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.scene;

import org.ddogleg.nn.alg.KdTreeDistance;

/**
 * Distance for word histograms
 *
 * @author Peter Abeles
 */
public class KdTreeHistogramScene_F64 implements KdTreeDistance<HistogramScene> {
	int N ;

	public KdTreeHistogramScene_F64(int n) {
		N = n;
	}

	@Override
	public double distance(HistogramScene a, HistogramScene b) {
		final int N = a.histogram.length;
		double total = 0;
		for( int i = 0; i < N; i++ ) {
			double d = a.histogram[i]-b.histogram[i];
			total += d*d;
		}

		return total;
	}

	@Override
	public double valueAt(HistogramScene point, int index) {
		return point.histogram[index];
	}

	@Override
	public int length() {
		return N;
	}
}
