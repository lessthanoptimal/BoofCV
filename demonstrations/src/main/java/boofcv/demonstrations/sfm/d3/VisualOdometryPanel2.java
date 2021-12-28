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

import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.d3.Orientation3D;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Controls for {@link VisualizeDepthVisualOdometryApp}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisualOdometryPanel2
		extends StandardAlgConfigPanel
		implements ItemListener, ActionListener, ChangeListener {
	JComboBox selectView;

	Orientation3D orientation = new Orientation3D();

	JTextArea displayIntegral;
	JTextArea displayOrigin;

	JTextArea displayFaults;
	JTextArea displayFps;
	JTextArea displayFrameNumber;

	JCheckBox showAll;
	JCheckBox showInliers;

	JSpinner spinnerStopFrame;
	JButton buttonPause = new JButton("Pause");
	JButton buttonStep = new JButton("Step");

	boolean setShowAll = true;
	boolean setShowInliers = false;

	int stopFrame = -1;

	boolean paused;
	@Nullable Se3_F64 prevToWorld;
	double integral;

	Listener listener;

	public VisualOdometryPanel2( Type type ) {

		if (type == Type.STEREO)
			selectView = new JComboBox(new String[]{"Right", "3D"});
		else if (type == Type.DEPTH)
			selectView = new JComboBox(new String[]{"Depth", "3D"});
		else if (type == Type.MONO_PLANE)
			selectView = new JComboBox(new String[]{"2D", "3D"});
		selectView.addActionListener(this);
		selectView.setMaximumSize(selectView.getPreferredSize());

		showAll = new JCheckBox("Show All");
		showAll.addItemListener(this);
		showAll.setSelected(setShowAll);
		showInliers = new JCheckBox("Show Inliers");
		showInliers.addItemListener(this);
		showInliers.setSelected(setShowInliers);

		displayIntegral = createTextInfo();
		displayOrigin = createTextInfo();

		displayFaults = createTextInfo();
		displayFps = createTextInfo();
		displayFrameNumber = createTextInfo();

		spinnerStopFrame = new JSpinner(new SpinnerNumberModel(stopFrame, -1, 9999, 1));
		spinnerStopFrame.addChangeListener(this);
		spinnerStopFrame.setMaximumSize(spinnerStopFrame.getPreferredSize());
		buttonPause.addActionListener(this);
		buttonStep.addActionListener(this);

		addLabeled(selectView, "View");
		addAlignCenter(buttonPause);
		addAlignCenter(buttonStep);
		addSeparator(150);
		addLabeled(displayFrameNumber, "Frame");
		addLabeled(displayFps, "FPS");
		addLabeled(displayFaults, "Faults");
		addLabeled(displayIntegral, "Integral");
		addLabeled(displayOrigin, "Origin");
		addAlignCenter(orientation);
		addSeparator(150);
		addCenterLabel("Stop Frame");
		addAlignCenter(spinnerStopFrame);
		addSeparator(150);
		addAlignLeft(showAll);
		addAlignLeft(showInliers);
		addVerticalGlue();
	}

	public void reset() {
		prevToWorld = null;
		integral = 0;
	}

	private JTextArea createTextInfo() {
		JTextArea comp = new JTextArea(1, 6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		return comp;
	}

	@Override
	public void itemStateChanged( ItemEvent e ) {
		if (e.getItem() == showInliers) {
			setShowInliers = e.getStateChange() != ItemEvent.DESELECTED;
		} else if (e.getItem() == showAll) {
			setShowAll = e.getStateChange() != ItemEvent.DESELECTED;
		}
	}

	public void setCameraToWorld( Se3_F64 cameraToWorld ) {

		Vector3D_F64 v = new Vector3D_F64(0, 0, 1);
		GeometryMath_F64.mult(cameraToWorld.getR(), v, v);

		orientation.setVector(v);
		orientation.repaint();

		displayOrigin.setText(String.format("%6.1f", cameraToWorld.getT().norm()));

		if (prevToWorld == null) {
			prevToWorld = cameraToWorld.copy();
		} else {
			Se3_F64 worldToPrev = prevToWorld.invert(null);
			cameraToWorld.concat(worldToPrev, prevToWorld);
			integral += prevToWorld.getT().norm();
			prevToWorld.setTo(cameraToWorld);
		}
		displayIntegral.setText(String.format("%6.1f", integral));
	}

	public void setFrameNumber( int frame ) {
		displayFrameNumber.setText(String.format("%5d", frame));
	}

	public void setNumFaults( int totalFaults ) {
		displayFaults.setText(String.format("%5d", totalFaults));
	}

	public void setFps( double fps ) {
		displayFps.setText(String.format("%6.1f", fps));
	}

	public boolean isShowAll() {
		return setShowAll;
	}

	public boolean isShowInliers() {
		return setShowInliers;
	}

	public int getStopFrame() {
		return stopFrame;
	}

	public Listener getListener() {
		return listener;
	}

	public void setListener( Listener listener ) {
		this.listener = listener;
	}

	public boolean isPaused() {
		return paused;
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		if (e.getSource() == selectView) {
			if (listener != null)
				listener.eventVoPanel(selectView.getSelectedIndex());
		} else if (e.getSource() == buttonPause) {
			setPaused(!paused);
			listener.handlePausedToggle();
		} else if (e.getSource() == buttonStep) {
			listener.handleStep();
		}
	}

	@Override
	public void stateChanged( ChangeEvent e ) {
		if (e.getSource() == spinnerStopFrame) {
			stopFrame = ((Number)spinnerStopFrame.getValue()).intValue();
		}
	}

	public void setPaused( boolean paused ) {
		if (this.paused != paused) {
			if (paused) {
				buttonPause.setText("Resume");
			} else {
				buttonPause.setText("Pause");
			}
			this.paused = paused;
		}
	}

	public interface Listener {
		void eventVoPanel( int view );

		void handlePausedToggle();

		void handleStep();
	}

	public static enum Type {
		STEREO,
		DEPTH,
		MONO_PLANE
	}
}
