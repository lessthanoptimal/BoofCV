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

package boofcv.alg.feature.detect.edge;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Provides methods for adjusting the settings in a canny edge detector
 *
 * @author Peter Abeles
 */
public class CannyControlBar extends JPanel implements ChangeListener {
	JSpinner controlBlur;
	JSlider controlThreshold;

	int blurRadius;
	int threshold;

	Listener listener;

	public CannyControlBar( int blurRadius , int threshold ) {
		this.blurRadius = blurRadius;
		this.threshold = threshold;

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		controlBlur = new JSpinner(new SpinnerNumberModel(blurRadius,1,20,1));
		controlBlur.addChangeListener(this);
		controlBlur.setPreferredSize(new Dimension((int)controlBlur.getPreferredSize().getWidth(),(int)controlBlur.getPreferredSize().getHeight()+8));
		controlBlur.setMaximumSize(controlBlur.getPreferredSize());

		controlThreshold = new JSlider(JSlider.HORIZONTAL,5,100,threshold);
		controlThreshold.setMajorTickSpacing(20);
		controlThreshold.setPaintTicks(true);
		controlThreshold.addChangeListener(this);

		add(new JLabel("Blur Radius"));
		add(controlBlur);
		add(new JLabel("Threshold"));
		add(controlThreshold);
		add(Box.createHorizontalGlue());
	}

	@Override
	public void stateChanged(ChangeEvent e) {

		if( controlBlur == e.getSource() ) {
			blurRadius = ((Number)controlBlur.getValue()).intValue();
		}  else if( controlThreshold == e.getSource() ) {
			threshold = ((Number)controlThreshold.getValue()).intValue();
		}

		if( listener != null )
			listener.changeCanny();
	}

	public int getBlurRadius() {
		return blurRadius;
	}

	public int getThreshold() {
		return threshold;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public static interface Listener
	{
		void changeCanny();
	}
}
