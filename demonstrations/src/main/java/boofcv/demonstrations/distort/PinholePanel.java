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

package boofcv.demonstrations.distort;

import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.controls.JSpinnerNumber;
import boofcv.struct.calib.CameraPinholeBrown;

import javax.swing.event.ChangeEvent;
import java.util.Objects;

/**
 * Panel which lets you select all the pinhole with distortion camera parameters
 *
 * @author Peter Abeles
 */
public class PinholePanel extends StandardAlgConfigPanel {
	JSpinnerNumber selectWidth = spinnerWrap(500, 0, 10_000, 10);
	JSpinnerNumber selectHeight = spinnerWrap(500, 0, 10_000, 10);
	JSpinnerNumber selectFX = spinnerWrap(50, 0.01, 1000.0, 1);
	JSpinnerNumber selectFY = spinnerWrap(50, 0.01, 1000.0, 1);
	JSpinnerNumber selectCX = spinnerWrap(50, 0.0, 100.0, 1);
	JSpinnerNumber selectCY = spinnerWrap(50, 0.0, 100.0, 1);
	JSpinnerNumber selectSkew = spinnerWrap(0, -2.0, 2.0, 0.1);
	JSpinnerNumber selectT1 = spinnerWrap(0, -0.2, 0.2, 0.01);
	JSpinnerNumber selectT2 = spinnerWrap(0, -0.2, 0.2, 0.01);
	JSpinnerNumber selectR1 = spinnerWrap(0, -10.0, 10.0, 0.05);
	JSpinnerNumber selectR2 = spinnerWrap(0, -10.0, 10.0, 0.05);
	JSpinnerNumber selectR3 = spinnerWrap(0, -10.0, 10.0, 0.05);
	JSpinnerNumber selectR4 = spinnerWrap(0, -10.0, 10.0, 0.05);

	public Listener listener;

	public PinholePanel() {
		this((m)->{});
	}

	public PinholePanel( Listener listener ) {
		this.listener = listener;

		addLabeled(selectWidth.spinner, "Image Width: ");
		addLabeled(selectHeight.spinner, "Image Height: ");
		addLabeled(selectFX.spinner, "fx: ");
		addLabeled(selectFY.spinner, "fy: ");
		addLabeled(selectCX.spinner, "cx: ");
		addLabeled(selectCY.spinner, "cy: ");
		addLabeled(selectSkew.spinner, "skew: ");
		addLabeled(selectT1.spinner, "t1: ");
		addLabeled(selectT2.spinner, "t2: ");
		addLabeled(selectR1.spinner, "r1: ");
		addLabeled(selectR2.spinner, "r2: ");
		addLabeled(selectR3.spinner, "r3: ");
		addLabeled(selectR4.spinner, "r4: ");
	}

	public void setCameraModel( CameraPinholeBrown model ) {
		// disable broadcasting of changes
		Listener original = this.listener;
		this.listener = (m)->{};

		double[] radial = Objects.requireNonNull(model.radial);

		selectWidth.spinner.setValue(model.width);
		selectHeight.spinner.setValue(model.height);
		selectFX.spinner.setValue(100.0*model.fx/model.width);
		selectFY.spinner.setValue(100.0*model.fx/model.width);
		selectCX.spinner.setValue(100.0*model.cx/model.width);
		selectCY.spinner.setValue(100.0*model.cy/model.height);
		selectSkew.spinner.setValue(model.skew);
		selectT1.spinner.setValue(model.t1);
		selectT2.spinner.setValue(model.t2);
		selectR1.spinner.setValue(radial[0]);
		selectR2.spinner.setValue(radial[1]);
		selectR3.spinner.setValue(radial[2]);
		selectR4.spinner.setValue(radial[3]);

		// re-enable
		this.listener = original;
	}

	public CameraPinholeBrown getCameraModel() {
		var model = new CameraPinholeBrown(4);

		double[] radial = Objects.requireNonNull(model.radial);

		model.width = selectWidth.vint();
		model.height = selectHeight.vint();
		model.fx = model.width*selectFX.vdouble()/100.0;
		model.fy = model.width*selectFY.vdouble()/100.0;
		model.cx = model.width*selectCX.vdouble()/100.0;
		model.cy = model.height*selectCY.vdouble()/100.0;
		model.skew = selectSkew.vdouble();
		model.t1 = selectT1.vdouble();
		model.t2 = selectT2.vdouble();
		radial[0] = selectR1.vdouble();
		radial[1] = selectR2.vdouble();
		radial[2] = selectR3.vdouble();
		radial[3] = selectR4.vdouble();

		return model;
	}

	@Override public void stateChanged( ChangeEvent e ) {
		listener.updatedPinholeModel(getCameraModel());
	}

	@FunctionalInterface public interface Listener {
		void updatedPinholeModel( CameraPinholeBrown model );
	}
}
