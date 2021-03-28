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

package boofcv.alg.structure;

import boofcv.misc.BoofMiscOps;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Common parent for metric and projective expand scene by one. Mostly contains functions for selecting which of the
 * known views it should use
 *
 * @author Peter Abeles
 */
public abstract class ExpandByOneView implements VerbosePrint {
	// If not null then print debugging information
	protected PrintStream verbose;

	// Reference to the working scene graph
	protected SceneWorkingGraph workGraph;

	/** Common algorithms for reconstructing the projective scene */
	protected @Getter @Setter PairwiseGraphUtils utils;

	//------------------------- Local work space

	// candidates for being used as known connections
	protected List<PairwiseImageGraph.Motion> validCandidates = new ArrayList<>();

	protected ExpandByOneView(ConfigProjectiveReconstruction configProjective) {
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

		double bestScore = 0.0;
		for (int connectionCnt = 0; connectionCnt < validCandidates.size(); connectionCnt++) {
			PairwiseImageGraph.Motion connectB = validCandidates.get(connectionCnt);
			PairwiseImageGraph.Motion connectC = findBestCommon(target, connectB, validCandidates);
			if (connectC == null)
				continue; // no common connection could be found
			PairwiseImageGraph.View viewB = connectB.other(target);
			PairwiseImageGraph.View viewC = connectC.other(target);

			PairwiseImageGraph.Motion connectBtoC = viewB.findMotion(viewC);
			checkTrue(connectBtoC != null, "BUG");

			double score = BoofMiscOps.min(connectB.score3D, connectC.score3D, connectBtoC.score3D);

			if (score > bestScore) {
				bestScore = score;
				connections.clear();
				connections.add(connectB);
				connections.add(connectC);
			}
		}
		if (verbose != null) verbose.printf("best selected pair score=%f\n", bestScore);

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

	/**
	 * Selects the view C which has the best connection from A to C and B to C. Best is defined using the
	 * scoring function and being 3D.
	 *
	 * @param viewA (input) The root node all motions must connect to
	 * @param connAB (input) A connection from view A to view B
	 * @param validConnections (input) List of connections that are known to be valid potential solutions
	 * @return The selected common view. null if none could be found
	 */
	public PairwiseImageGraph.Motion findBestCommon( PairwiseImageGraph.View viewA, PairwiseImageGraph.Motion connAB, List<PairwiseImageGraph.Motion> validConnections ) {
		double bestScore = 0.0;
		PairwiseImageGraph.Motion bestConnection = null;

		PairwiseImageGraph.View viewB = connAB.other(viewA);

		for (int connIdx = 0; connIdx < validConnections.size(); connIdx++) {
			PairwiseImageGraph.Motion connAC = validConnections.get(connIdx);
			if (connAC == connAB)
				continue;
			PairwiseImageGraph.View viewC = connAC.other(viewA);

			// The views must form a complete loop with 3D information
			PairwiseImageGraph.Motion connBC = viewB.findMotion(viewC);
			if (null == connBC || !connBC.is3D)
				continue;

			// Maximize worst case 3D information
			double score = Math.min(connAC.score3D, connBC.score3D);

			if (score > bestScore) {
				bestScore = score;
				bestConnection = connAC;
			}
		}

		return bestConnection;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}
}
