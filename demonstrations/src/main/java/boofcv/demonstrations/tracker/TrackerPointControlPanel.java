/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.tracker;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Control panel for {@link VideoTrackerPointFeaturesApp}
 *
 * @author Peter Abeles
 */
public class TrackerPointControlPanel
		extends StandardAlgConfigPanel
		implements ActionListener , ChangeListener
{
	JLabel labelSize = new JLabel();
	JLabel labelTimeMS = new JLabel();
	JLabel labelTrackCount = new JLabel();

	// Which algorithm to run
	JComboBox comboAlg;

	// Spawn features when tracks drop below this value
	JSpinner spinnerMinFeats;
	JSpinner spinnerMaxFeats;

	// How wide a feature being tracked is
	JSpinner spinnerWidth;

	public int algorithm=0;
	public int featWidth = 5;
	public int minFeatures = 200;
	public int maxFeatures = 800;

	Listener listener;

	public TrackerPointControlPanel( Listener listener ) {
		this.listener = listener;

		comboAlg = combo(algorithm,"KLT","ST-BRIEF","ST-NCC","FH-SURF","ST-SURF-KLT","FH-SURF-KLT");
		spinnerWidth = spinner(5,3,41,2);
		spinnerMinFeats = spinner(minFeatures,50,2000,10);
		spinnerMaxFeats = spinner(maxFeatures,50,2000,10);

		addLabeled(labelSize,"Size");
		addLabeled(labelTimeMS,"Time");
		addLabeled(labelTrackCount,"Tracks");
		addAlignCenter(comboAlg);
		addLabeled(spinnerWidth,"Feat Width");
		addLabeled(spinnerMinFeats,"Min. Feats");
		addLabeled(spinnerMaxFeats,"Max. Feats");
	}

	public void setImageSize( int width , int height ) {
		labelSize.setText(String.format("%d x %d",width,height));
	}

	public void setTime( double time ) {
		labelTimeMS.setText(String.format("%7.1f (ms)",time));
	}

	public void setTrackCount( int count ) {
		labelTrackCount.setText(""+count);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == comboAlg ) {
			algorithm = comboAlg.getSelectedIndex();
		}
		listener.handleAlgorithmUpdated();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( e.getSource() == spinnerMinFeats ) {
			minFeatures = ((Number)spinnerMinFeats.getValue()).intValue();
		} else if( e.getSource() == spinnerMaxFeats ) {
			maxFeatures = ((Number)spinnerMaxFeats.getValue()).intValue();
			listener.handleAlgorithmUpdated();
		} else if( e.getSource() == spinnerWidth ) {
			featWidth = ((Number)spinnerWidth.getValue()).intValue();
			listener.handleAlgorithmUpdated();
		}
	}

	public interface Listener {
		void handleAlgorithmUpdated();
	}
}
