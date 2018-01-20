/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.distort;

import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.calib.CameraPinholeRadial;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Panel which lets you select all the pinhole with distortion camera parameters
 *
 * @author Peter Abeles
 */
public class PinholePanel extends StandardAlgConfigPanel
	implements ChangeListener
{
	JSpinner selectWidth;
	JSpinner selectHeight;
	JSpinner selectFX;
	JSpinner selectFY;
	JSpinner selectCX;
	JSpinner selectCY;
	JSpinner selectSkew;
	JSpinner selectT1;
	JSpinner selectT2;
	JSpinner selectR1;
	JSpinner selectR2;

	CameraPinholeRadial original;
	CameraPinholeRadial adjusted = new CameraPinholeRadial();

	Listener listener;

	public PinholePanel(CameraPinholeRadial original ,
						Listener listener ) {
		this.original = original;
		this.adjusted.set(original);
		this.listener = listener;

		selectWidth = spinner(original.width, 60, 1200, 10);
		selectHeight = spinner(original.height, 60, 1200, 10);
		selectFX = spinner(original.fx, 10.0, 2000, 10);
		selectFY = spinner(original.fy, 10.0, 2000, 10);
		selectCX = spinner((int)(100.0*original.cx/(double)original.width) , 0, 100, 1);
		selectCY = spinner((int)(100.0*original.cy/(double)original.height), 0, 100, 1);
		selectSkew = spinner(original.skew, -500, 500, 10);
		selectT1 = spinner(original.t1, -0.2, 0.2, 0.01);
		configureSpinnerFloat(selectT1, 1,3);
		selectT2 = spinner(original.t2, -0.2, 0.2, 0.01);
		configureSpinnerFloat(selectT2, 1,3);
		selectR1 = spinner(original.radial[0], -3.0, 3.0, 0.05);
		configureSpinnerFloat(selectR1, 1,3);
		selectR2 = spinner(original.radial[1], -3.0, 3.0, 0.05);
		configureSpinnerFloat(selectR2, 1,3);

		addLabeled(selectWidth,  "Image Width: ", this);
		addLabeled(selectHeight, "Image Height: ", this);
		addLabeled(selectFX,     "fx: ", this);
		addLabeled(selectFY,     "fy: ", this);
		addLabeled(selectCX,     "cx: ", this);
		addLabeled(selectCY,     "cy: ", this);
		addLabeled(selectSkew,   "skew: ", this);
		addLabeled(selectT1,     "t1: ", this);
		addLabeled(selectT2,     "t2: ", this);
		addLabeled(selectR1,     "r1: ", this);
		addLabeled(selectR2,     "r2: ", this);

	}

	public void setCameraModel( CameraPinholeRadial original ) {
		this.original = original;
		this.adjusted.set(original);

		// disable notifications for a moment
		int N = getComponentCount();
		for (int i = 0; i < N; i++) {
			try {
				JSpinner spinner = (JSpinner)getComponent(i);
				spinner.removeChangeListener(this);
			} catch( RuntimeException ignore){}
		}

		selectWidth.setValue(original.width);
		selectHeight.setValue(original.height);
		selectFX.setValue(original.fx);
		selectFY.setValue(original.fy);
		selectCX.setValue((int)(100.0*original.cx/original.width));
		selectCY.setValue((int)(100.0*original.cy/original.height));
		selectSkew.setValue(original.skew);
		selectT1.setValue(original.t1);
		selectT2.setValue(original.t2);
		selectR1.setValue(original.radial[0]);
		selectR2.setValue(original.radial[1]);

		// re-enabled notifications
		for (int i = 0; i < N; i++) {
			try {
				JSpinner spinner = (JSpinner)getComponent(i);
				spinner.addChangeListener(this);
			} catch( RuntimeException ignore){}
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( selectWidth == e.getSource() ) {
			adjusted.width = ((Number) selectWidth.getValue()).intValue();
			int percent =  ((Number) selectCX.getValue()).intValue();
			adjusted.cx = adjusted.width*percent/100.0;
		} else if( selectHeight == e.getSource() ) {
			adjusted.height = ((Number) selectHeight.getValue()).intValue();
			int percent =  ((Number) selectCY.getValue()).intValue();
			adjusted.cy = adjusted.height*percent/100.0;
		} else if( selectFX == e.getSource() ) {
			adjusted.fx =  ((Number) selectFX.getValue()).doubleValue();
		} else if( selectFY == e.getSource() ) {
			adjusted.fy =  ((Number) selectFY.getValue()).doubleValue();
		} else if( selectCX == e.getSource() ) {
			int percent =  ((Number) selectCX.getValue()).intValue();
			adjusted.cx = adjusted.width*percent/100.0;
		} else if( selectCY == e.getSource() ) {
			int percent =  ((Number) selectCY.getValue()).intValue();
			adjusted.cy = adjusted.height*percent/100.0;
		} else if( selectSkew == e.getSource() ) {
			adjusted.skew =  ((Number) selectSkew.getValue()).doubleValue();
		} else if( selectT1 == e.getSource() ) {
			adjusted.t1 =  ((Number) selectT1.getValue()).doubleValue();
		} else if( selectT2 == e.getSource() ) {
			adjusted.t2 =  ((Number) selectT2.getValue()).doubleValue();
		} else if( selectR1 == e.getSource() ) {
			adjusted.radial[0] =  ((Number) selectR1.getValue()).doubleValue();
		} else if( selectR2 == e.getSource() ) {
			adjusted.radial[1] =  ((Number) selectR2.getValue()).doubleValue();
		} else {
			return;
		}
		listener.updatedPinholeModel(adjusted);
	}

	public CameraPinholeRadial getDesired() {
		return adjusted;
	}

	public interface Listener {
		void updatedPinholeModel(CameraPinholeRadial model );
	}
}
