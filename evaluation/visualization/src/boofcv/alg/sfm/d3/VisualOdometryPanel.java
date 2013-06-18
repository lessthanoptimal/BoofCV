/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d3;

import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.d3.Orientation3D;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author Peter Abeles
 */
public class VisualOdometryPanel
		extends StandardAlgConfigPanel
		implements ItemListener, ActionListener
{
	JLabel displayStatus;

	JComboBox selectView;

	JTextArea displayX;
	JTextArea displayY;
	JTextArea displayZ;

	Orientation3D orientation = new Orientation3D();

	JTextArea displayIntegral;
	JTextArea displayOrigin;

	JTextArea displayTracks;
	JTextArea displayInliers;
	JTextArea displayFaults;
	JTextArea displayFps;

	JCheckBox showAll;
	JCheckBox showInliers;

	boolean setShowAll = true;
	boolean setShowInliers = false;

	Se3_F64 prevToWorld;
	double integral;

	Listener listener;

	public VisualOdometryPanel( Type type ) {
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		displayStatus = new JLabel();
		displayStatus.setFont(new Font("Dialog",Font.BOLD,16));

		if( type == Type.STEREO )
			selectView = new JComboBox(new String[]{"Right","3D"});
		else if( type == Type.DEPTH )
			selectView = new JComboBox(new String[]{"Depth","3D"});
		else if( type == Type.MONO_PLANE )
			selectView = new JComboBox(new String[]{"2D","3D"});
		selectView.addActionListener(this);
		selectView.setMaximumSize(selectView.getPreferredSize());

		displayX = createTextInfo();
		displayY = createTextInfo();
		displayZ = createTextInfo();

		showAll = new JCheckBox("Show All");
		showAll.addItemListener(this);
		showAll.setSelected(setShowAll);
		showInliers = new JCheckBox("Show Inliers");
		showInliers.addItemListener(this);
		showInliers.setSelected(setShowInliers);

		displayIntegral = createTextInfo();
		displayOrigin = createTextInfo();

		displayTracks = createTextInfo();
		displayInliers = createTextInfo();
		displayFaults = createTextInfo();
		displayFps = createTextInfo();

		addAlignCenter(displayStatus, this);
		addLabeled(selectView,  "View", this);
		addSeparator(150);
		addLabeled(displayTracks,  "Tracks", this);
		addLabeled(displayInliers, "Inliers", this);
		addLabeled(displayFaults, "Faults", this);
		addLabeled(displayFps, "FPS", this);
		addSeparator(150);
		addLabeled(displayX, "X ", this);
		addLabeled(displayY, "Y ", this);
		addLabeled(displayZ, "Z ", this);
		addAlignCenter(orientation, this);
		addSeparator(150);
		addLabeled(displayIntegral, "Integral", this);
		addLabeled(displayOrigin, "Origin", this);
		addSeparator(150);
		addAlignLeft(showAll, this);
		addAlignLeft(showInliers, this);
		addVerticalGlue(this);
	}

	public void reset() {
		prevToWorld = null;
		integral = 0;
	}

	private JTextArea createTextInfo() {
		JTextArea comp = new JTextArea(1,6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		return comp;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( e.getItem() == showInliers) {
			setShowInliers = e.getStateChange() != ItemEvent.DESELECTED;
		} else if( e.getItem() == showAll) {
			setShowAll = e.getStateChange() != ItemEvent.DESELECTED;
		}
	}

	public void setCameraToWorld(Se3_F64 cameraToWorld) {
		displayX.setText(String.format("%6.1f",cameraToWorld.getT().x));
		displayY.setText(String.format("%6.1f",cameraToWorld.getT().y));
		displayZ.setText(String.format("%6.1f",cameraToWorld.getT().z));

		Vector3D_F64 v = new Vector3D_F64(0,0,1);
		GeometryMath_F64.mult(cameraToWorld.getR(), v, v);

		orientation.setVector(v);
		orientation.repaint();

		displayOrigin.setText(String.format("%6.1f",cameraToWorld.getT().norm()));

		if( prevToWorld == null ) {
			prevToWorld = cameraToWorld.copy();
		} else {
			Se3_F64 worldToPrev = prevToWorld.invert(null);
			cameraToWorld.concat(worldToPrev, prevToWorld);
			integral += prevToWorld.getT().norm();
			prevToWorld.set(cameraToWorld);
		}
		displayIntegral.setText(String.format("%6.1f",integral));
	}

	public void setStatus( String text , Color color ) {
		displayStatus.setText(text);
		displayStatus.setForeground(color);
	}

	public void setNumTracks( int totalTracks ) {
		displayTracks.setText(String.format("%5d",totalTracks));
	}

	public void setNumInliers(int totalInliers) {
		displayInliers.setText(String.format("%5d",totalInliers));
	}

	public void setNumFaults(int totalFaults) {
		displayFaults.setText(String.format("%5d",totalFaults));
	}

	public void setFps(double fps) {
		displayFps.setText(String.format("%6.1f",fps));
	}

	public boolean isShowAll() {
		return setShowAll;
	}

	public boolean isShowInliers() {
		return setShowInliers;
	}

	public Listener getListener() {
		return listener;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == selectView ) {
			if( listener != null )
				listener.eventVoPanel(selectView.getSelectedIndex());
		}
	}

	public static interface Listener {
		public void eventVoPanel( int view );
	}

	public static enum Type {
		STEREO,
		DEPTH,
		MONO_PLANE
	}
}
