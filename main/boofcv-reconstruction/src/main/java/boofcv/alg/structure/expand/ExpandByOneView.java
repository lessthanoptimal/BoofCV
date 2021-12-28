/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.structure.expand;

import boofcv.alg.structure.ConfigProjectiveReconstruction;
import boofcv.alg.structure.PairwiseGraphUtils;
import boofcv.alg.structure.PairwiseImageGraph;
import boofcv.alg.structure.SceneWorkingGraph;
import boofcv.misc.BoofMiscOps;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Common parent for metric and projective expand scene by one. Mostly contains functions for selecting which of the
 * known views it should use
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class ExpandByOneView implements VerbosePrint {
	// If not null then print debugging information
	protected @Nullable PrintStream verbose;

	// Reference to the working scene graph
	protected SceneWorkingGraph workGraph;

	/** Common algorithms for reconstructing the projective scene */
	public @Getter @Setter PairwiseGraphUtils utils;

	//------------------------- Local work space

	// candidates for being used as known connections
	protected List<PairwiseImageGraph.Motion> validCandidates = new ArrayList<>();

	protected ExpandByOneView( ConfigProjectiveReconstruction configProjective) {
		utils = new PairwiseGraphUtils(configProjective);
	}

	protected ExpandByOneView() {
		this(new ConfigProjectiveReconstruction());
	}

	/**
	 * Selects two views which are connected to the target by maximizing a score function. The two selected
	 * views must have 3D information, be connected to each other, and have a known camera matrix. These three views
	 * will then be used to estimate a trifocal tensor
	 *
	 * @param target (input) A view
	 * @param connections (output) the two selected connected views to the target
	 * @return true if successful or false if it failed
	 */
	public boolean selectTwoConnections( PairwiseImageGraph.View target, List<PairwiseImageGraph.Motion> connections ) {
		connections.clear();

		// Create a list of connections in the target that can be used
		createListOfValid(target, validCandidates);

		int considered = 0;
		double bestScore = 0.0;
		for (int connIdxB = 0; connIdxB < validCandidates.size(); connIdxB++) {
			PairwiseImageGraph.Motion connectB = validCandidates.get(connIdxB);
			PairwiseImageGraph.View viewB = connectB.other(target);

			// Skip if it can't possibly have a better score
			if (connectB.score3D < bestScore) {
				continue;
			}

			for (int connIdxC = connIdxB+1; connIdxC < validCandidates.size(); connIdxC++) {
				PairwiseImageGraph.Motion connectC = validCandidates.get(connIdxC);
				PairwiseImageGraph.View viewC = connectC.other(target);

				PairwiseImageGraph.Motion connectBtoC = viewB.findMotion(viewC);
				if (connectBtoC==null || !connectBtoC.is3D)
					continue;

				double score = BoofMiscOps.min(connectB.score3D, connectC.score3D, connectBtoC.score3D);

				considered++;
				if (score > bestScore) {
					bestScore = score;
					connections.clear();
					connections.add(connectB);
					connections.add(connectC);
				}
			}
		}
		if (verbose != null)
			verbose.printf("best pair: score=%.2f from pairs=%d views.size=%d\n",
					bestScore, considered, validCandidates.size());

		return !connections.isEmpty();
	}

	/**
	 * Finds all the connections from the target view which are 3D and have known other views
	 *
	 * @param target (input)
	 * @param validConnections (output)
	 */
	void createListOfValid( PairwiseImageGraph.View target, List<PairwiseImageGraph.Motion> validConnections ) {
		validConnections.clear();
		for (int connectionIdx = 0; connectionIdx < target.connections.size; connectionIdx++) {
			PairwiseImageGraph.Motion connectB = target.connections.get(connectionIdx);
			if (!connectB.is3D || !workGraph.isKnown(connectB.other(target)))
				continue;
			validConnections.add(connectB);
		}
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
