/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.interest.benchmark;

import jgrl.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
public class StabilityMetric {
	List<Point2D_I32> original;
	List<Double> error = new ArrayList<Double>();
	List<Integer> numMissed = new ArrayList<Integer>();
	List<Integer> numMatched = new ArrayList<Integer>();

	List<Score> scores = new ArrayList<Score>();

	public void setOriginal(List<Point2D_I32> found ) {
		this.original = found;
	}

	public void reset() {
		error.clear();
		numMissed.clear();
		numMatched.clear();
	}

	public Score computeScore() {
		double totalMatched = 0;
		double totalMissed = 0;
		double totalScore = 0;
		int scoreDivisor = 0;

		for( int i = 0; i < error.size(); i++ ) {
			totalMissed += numMissed.get(i);
			totalMatched += numMatched.get(i);

			double s = error.get(i);
			if( Double.isNaN(s) || Double.isInfinite(s))
				continue;
			totalScore += s;
			scoreDivisor++;
		}

		totalMatched /= error.size();
		totalMissed /= error.size();
		totalScore /= scoreDivisor;
		Score ret = new Score();
		ret.aveMatched = totalMatched;
		ret.error = totalScore;
		ret.fracMissed = totalMissed/(totalMissed+totalMatched);

		return ret;
	}

	public static class Score
	{
		double fracMissed;
		double error;
		double aveMatched;
	}
}
