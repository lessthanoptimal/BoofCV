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

package boofcv.demonstrations.sfm.d3;

import javax.swing.*;

/**
 * Controls for {@link VisualizeDepthVisualOdometryApp}.
 *
 * @author Peter Abeles
 */
public class VisualOdometryFeatureTrackerPanel extends VisualOdometryAlgorithmPanel {
	JTextArea displayTracks;
	JTextArea displayInliers;

	public VisualOdometryFeatureTrackerPanel() {
		displayTracks = createTextInfo();
		displayInliers = createTextInfo();

		addLabeledV(displayTracks, "Tracks", this);
		addLabeledV(displayInliers, "Inliers", this);
		addHorizontalGlue(this);
	}

	public void setNumTracks( int totalTracks ) {
		displayTracks.setText(String.format("%5d", totalTracks));
	}

	public void setNumInliers( int totalInliers ) {
		displayInliers.setText(String.format("%5d", totalInliers));
	}

	private JTextArea createTextInfo() {
		JTextArea comp = new JTextArea(1, 6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		return comp;
	}
}
