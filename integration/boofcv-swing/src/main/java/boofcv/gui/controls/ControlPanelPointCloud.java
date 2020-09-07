/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.controls;

import boofcv.gui.StandardAlgConfigPanel;
import boofcv.misc.BoofLambdas;
import boofcv.visualize.*;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

/**
 * Control panel for adjusting how point clouds are visualzied
 *
 * @author Peter Abeles
 */
public class ControlPanelPointCloud extends StandardAlgConfigPanel {

	// which color in colorizers should it use
	int colorScheme = 0;

	// clip distance for rendering. Disabled if <= 0
	public double clipDistance = 0;
	// If it should use "fog" when rendering the cloud
	public boolean fog = false;

	// range 0 to 1000
	public int periodAdjust=500;
	public int offsetAdjust=500;
	public int speedAdjust=500;

	// color of the point cloud RGB
	protected int backgroundColor3D = 0x000000;

	// If the point cloud should be colorized or not
	protected final JComboBox comboColorizer = combo(colorScheme,"Color","X-YZ","Y-XZ","Z-XY","RB-X","RB-Y","RB-Z","RGB-X","RGB-Y","RGB-Z");
	// button which shows / selects the cloud's background
	protected final JButton bColorBackGround = new JButton();

	protected final JSlider sliderPeriodColor = slider(0,1000,periodAdjust,120);
	protected final JSlider sliderOffsetColor = slider(0,1000,offsetAdjust,120);
	protected final JSlider sliderSpeed3D = slider(0,1000,speedAdjust,120);

	protected JFormattedTextField fieldClip = textfield(clipDistance,0,Double.NaN,100);
	protected JCheckBox checkFog = checkbox("",fog,"Turn on/off fog");

	// callback for when the background color has changed
	protected @Setter BoofLambdas.ProcessCall callbackBackground = ()->{};
	// callback for when any other setting has changed
	protected @Setter BoofLambdas.ProcessCall callbackModified = ()->{};

	public ControlPanelPointCloud( BoofLambdas.ProcessCall callback ) {
		this();
		callbackBackground = callback;
		callbackModified = callback;
	}

	public ControlPanelPointCloud() {
		layoutControls();
	}

	protected void layoutControls() {
		JPanel clipPanel = new JPanel();
		clipPanel.setLayout(new BoxLayout(clipPanel,BoxLayout.X_AXIS));
		clipPanel.add(fieldClip);
		clipPanel.add(checkFog);

		addLabeled(createColorPanel(),"Color","Point cloud colorization method");
		addLabeled(clipPanel,"Clip","Set to a non-zero value to specify a max distance for rendering");
		addLabeled(sliderOffsetColor,"Offset", "Pseudo color offset of periodic function");
		addLabeled(sliderPeriodColor,"Period", "The pseudo color's period");
		addLabeled(sliderSpeed3D,"Speed","Adjust translational speed through point cloud");
	}

	public void configure(PointCloudViewer pcv , double periodBaseline, double translateBaseline ) {
		double periodColor = periodBaseline*periodScale();
		PeriodicColorizer colorizer=null;
		switch( colorScheme ) {
			case 0: pcv.removeColorizer();break;
			case 1: colorizer = new TwoAxisRgbPlane.X_YZ(4.0); break;
			case 2: colorizer = new TwoAxisRgbPlane.Y_XZ(4.0); break;
			case 3: colorizer = new TwoAxisRgbPlane.Z_XY(4.0); break;
			case 4: colorizer = new SingleAxisMagentaBlue.X(); break;
			case 5: colorizer = new SingleAxisMagentaBlue.Y(); break;
			case 6: colorizer = new SingleAxisMagentaBlue.Z(); break;
			case 7: colorizer = new SingleAxisRgb.X(); break;
			case 8: colorizer = new SingleAxisRgb.Y(); break;
			case 9: colorizer = new SingleAxisRgb.Z(); break;
		}
		if( colorizer != null ) {
			colorizer.setPeriod(periodColor);
			colorizer.setOffset(offsetScale());
			pcv.setColorizer(colorizer);
		}
		pcv.setBackgroundColor(backgroundColor3D);
		pcv.setTranslationStep(speedScale()*translateBaseline);
		if( clipDistance > 0 ) {
			pcv.setClipDistance(clipDistance);
			pcv.setFog(fog);
		} else {
			pcv.setClipDistance(Double.MAX_VALUE);
			pcv.setFog(false);
		}
	}

	public void setEnabledAll( boolean enabled ) {
		comboColorizer.setEnabled(enabled);
		sliderOffsetColor.setEnabled(enabled);
		sliderPeriodColor.setEnabled(enabled);
		sliderSpeed3D.setEnabled(enabled);
		bColorBackGround.setEnabled(enabled);
		fieldClip.setEnabled(enabled);
		checkFog.setEnabled(enabled);
	}

	/**
	 * Button for selecting background color and another for selecting how to colorize
	 */
	private JPanel createColorPanel() {
		bColorBackGround.addActionListener(e->{
			Color newColor = JColorChooser.showDialog(
					ControlPanelPointCloud.this,
					"Background Color",
					new Color(getActiveBackgroundColor()));
			if( newColor == null )
				return;
			setColorButtonColor(newColor.getRGB());
			callbackBackground.process();
		});


		// Create a square which shows the background color
		bColorBackGround.setPreferredSize(new Dimension(20,20));
		bColorBackGround.setMinimumSize(bColorBackGround.getPreferredSize());
		bColorBackGround.setMaximumSize(bColorBackGround.getPreferredSize());
		bColorBackGround.setBorder(BorderFactory.createLineBorder(Color.WHITE));
		bColorBackGround.setIcon(new FlatIcon());
		bColorBackGround.setToolTipText("Click to change the background color");
		setColorButtonColor(backgroundColor3D);

		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.setBorder(BorderFactory.createEmptyBorder());
		p.add(bColorBackGround);
		p.add(Box.createRigidArea(new Dimension(5,5)));
		p.add(comboColorizer);
		return p;
	}

	protected int getActiveBackgroundColor() {
		return backgroundColor3D;
	}

	/**
	 * Changes the saved color and updates the button's color
	 */
	protected void setColorButtonColor( int color ) {
		backgroundColor3D = color;
		bColorBackGround.repaint();
	}

	@Override
	public void controlChanged(final Object source)
	{
		if( source == sliderPeriodColor ) {
			periodAdjust = sliderPeriodColor.getValue();
		} else if( source == sliderOffsetColor ) {
			offsetAdjust = sliderOffsetColor.getValue();
		} else if( source == sliderSpeed3D) {
			speedAdjust = sliderSpeed3D.getValue();
		} else if( source == comboColorizer) {
			colorScheme = comboColorizer.getSelectedIndex();
		} else if( source == checkFog ) {
			fog = checkFog.isSelected();
		} else if( source == fieldClip ) {
			clipDistance = ((Number)fieldClip.getValue()).doubleValue();
		} else {
			throw new RuntimeException("Egads");
		}
		callbackModified.process();
	}

	public double periodScale() {
		if( periodAdjust > 500 ) {
			double f = (periodAdjust-500)/500.0;
			return 1.0+f*10;
		} else if( periodAdjust < 500 ) {
			double f = (500-periodAdjust)/500.0;
			return 1.0-0.98*f;
		} else {
			return 1.0;
		}
	}

	public double offsetScale() {
		return (offsetAdjust-500)/500.0;
	}

	public double speedScale() {
		if( speedAdjust > 500 ) {
			double f = (speedAdjust-500)/500.0;
			return Math.pow(2.0,f*6);
		} else if( speedAdjust < 500 ) {
			double f = (500-speedAdjust)/500.0;
			return 1.0-0.98*f;
		} else {
			return 1.0;
		}
	}

	/**
	 * use an icon to fill in the color since it works better across different swing UI themes
	 */
	private class FlatIcon implements Icon {
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(new Color(getActiveBackgroundColor()));
			g.fillRect(0,0,c.getWidth(),c.getHeight());
		}

		@Override
		public int getIconWidth() {return 20;}

		@Override
		public int getIconHeight() {return 20;}
	}
}
