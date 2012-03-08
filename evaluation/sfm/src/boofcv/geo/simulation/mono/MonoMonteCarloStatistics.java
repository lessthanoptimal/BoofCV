/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.geo.simulation.mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class MonoMonteCarloStatistics {
	
	double drift50;
	double drift95;
	
	double location50;
	double location95;
	
	double rotation50;
	double rotation95;
	
	double aveFatal;
	int numException;
	
	double fps50;

	public MonoMonteCarloStatistics( List<MonoTrialResults> results ) {
		List<Double> drift = new ArrayList<Double>();
		List<Double> location = new ArrayList<Double>();
		List<Double> rotation = new ArrayList<Double>();
		List<Double> time = new ArrayList<Double>();

		for( MonoTrialResults r : results ) {
			drift.add(r.scaleDrift);
			location.add(r.translation);
			rotation.add(r.rotation);
			time.add(r.secondsPerFrame);

			if( r.exception )
				numException++;
			aveFatal += r.numFaults;
		}
		aveFatal /= results.size();

		Collections.sort(drift);
		Collections.sort(location);
		Collections.sort(rotation);

		drift50 = drift.get(results.size()/2);
		location50 = location.get(results.size()/2);
		rotation50 = rotation.get(results.size()/2);
		fps50 = 1.0/time.get(results.size()/2);

		drift95 = drift.get((int)(results.size()*0.95));
		location95 = location.get((int)(results.size()*0.95));
		rotation95 = rotation.get((int)(results.size()*0.95));
	}

}
