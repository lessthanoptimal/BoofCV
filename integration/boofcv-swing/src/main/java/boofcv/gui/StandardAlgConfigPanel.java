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

package boofcv.gui;

import boofcv.struct.ConfigLength;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 * Common base class for panels used for configuring the algorithms.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unused", "SameParameterValue"})
public class StandardAlgConfigPanel extends JPanel implements ActionListener, ChangeListener {

	public StandardAlgConfigPanel() {
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
	}

	/**
	 * Wraps the input panel inside another panel that will fill the control panel horizontally
	 */
	protected JPanel fillHorizontally( JPanel panel ) {
		JPanel hack = new JPanel(new BorderLayout()) {
			@Override
			public Dimension getPreferredSize() {
				Dimension d;
				try {
					d = StandardAlgConfigPanel.this.getPreferredSize();
				} catch( RuntimeException ignore ) {
					d = StandardAlgConfigPanel.this.getSize();
				}

				Dimension p = super.getPreferredSize();
				return new Dimension(d.width-4,p.height);
			}

			@Override
			public Dimension getMaximumSize() {
				return getPreferredSize();
			}
		};
		hack.add(BorderLayout.CENTER,panel);
		return hack;
	}

	protected JConfigLength configLength(ConfigLength initial , int min , int max ) {
		JConfigLength control = new JConfigLength((JConfigLength.Listener)this,true);
		control.setValue(initial);
		control.setLengthBounds(min,max);
		control.setMaximumSize(control.getPreferredSize());
		return control;
	}

	protected JButton button( String name , boolean enabled ) {
		return button(name,enabled,this);
	}

	protected JButton button( String name , boolean enabled , ActionListener listener ) {
		JButton b = new JButton(name);
		b.setEnabled(enabled);
		b.addActionListener(listener);
		return b;
	}

	protected JSlider slider( int min , int max , int initial , int widgetWidth ) {
		JSlider slider = new JSlider(min,max,initial);
		slider.setMaximumSize(new Dimension(widgetWidth,24));
		slider.setPreferredSize(slider.getMaximumSize());
		slider.addChangeListener(this);
		return slider;
	}

	protected JComboBox<String> combo( int initial , Object... items ) {
		JComboBox<String> c = new JComboBox<>();

		for (int i = 0; i < items.length; i++) {
			c.addItem(items[i].toString());
		}
		c.setSelectedIndex(initial);
		c.addActionListener(this);
		c.setMaximumSize(c.getPreferredSize());
		return c;
	}

	protected JComboBox<String> combo( ActionListener listener, int initial , Object... items ) {
		JComboBox<String> c = new JComboBox<>();

		for (int i = 0; i < items.length; i++) {
			c.addItem(items[i].toString());
		}
		c.setSelectedIndex(initial);
		if( listener != null )
			c.addActionListener(listener);
		c.setMaximumSize(c.getPreferredSize());
		return c;
	}

	protected JSpinner spinner(int initial , Object[] items ) {
		JSpinner spinner = new JSpinner(new SpinnerListModel(items));
		spinner.setValue(items[initial]);
		spinner.setMaximumSize(spinner.getPreferredSize());
		spinner.addChangeListener(this);
		return spinner;
	}

	protected JSpinner spinner(int initial , int minimum , int maximum, int stepSize , ChangeListener listener ) {
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, minimum, maximum, stepSize));
		spinner.setMaximumSize(spinner.getPreferredSize());
		spinner.addChangeListener(listener);
		return spinner;
	}

	protected JSpinner spinner(int initial , int minimum , int maximum, int stepSize ) {
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, minimum, maximum, stepSize));
		spinner.setMaximumSize(spinner.getPreferredSize());
		spinner.addChangeListener(this);
		return spinner;
	}

	protected JSpinner spinner( double initial , double minimum , double maximum, double stepSize ) {
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, minimum, maximum, stepSize));
		spinner.setMaximumSize(spinner.getPreferredSize());
		spinner.addChangeListener(this);
		return spinner;
	}

	protected JSpinner spinner( double initial , double minimum , double maximum, double stepSize,
								int integerDigits, int fractionDigits ) {
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, minimum, maximum, stepSize));
		JSpinner.NumberEditor editor = (JSpinner.NumberEditor)spinner.getEditor();
		DecimalFormat format = editor.getFormat();
		format.setMinimumFractionDigits(fractionDigits);
		format.setMinimumIntegerDigits(integerDigits);
		editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
		Dimension d = spinner.getPreferredSize();
		d.width = (integerDigits+1+fractionDigits)*12;
		spinner.setPreferredSize(d);
		spinner.setMaximumSize(d);
		// force it to render using the format specified above. A bit of a hack. Got a better idea?
		spinner.setValue(1-initial);spinner.setValue(initial);
		spinner.addChangeListener(this);
		return spinner;
	}

	protected JSpinner spinner( double initial , double minimum , double maximum, double stepSize,
								String formatString , int digits  ) {

		JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, minimum, maximum, stepSize));
		JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner,formatString);
		spinner.setEditor(editor);
		editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
		Dimension d = spinner.getPreferredSize();
		d.width = digits*9;
		spinner.setPreferredSize(d);
		spinner.setMaximumSize(d);
		// force it to render using the format specified above. A bit of a hack. Got a better idea?
		spinner.setValue(1-initial);spinner.setValue(initial);
		spinner.addChangeListener(this);
		return spinner;
	}

	protected void configureSpinnerFloat(JSpinner spinner, int integerDigits, int fractionDigits) {
		double min = ((Number)((SpinnerNumberModel)spinner.getModel()).getMinimum()).doubleValue();
		int adjust = min < 0 ? 1 : 0;
		JSpinner.NumberEditor editor = (JSpinner.NumberEditor)spinner.getEditor();
		DecimalFormat format = editor.getFormat();
		format.setMinimumFractionDigits(fractionDigits);
		format.setMinimumIntegerDigits(integerDigits);
		editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
		Dimension d = spinner.getPreferredSize();
		d.width = (adjust+integerDigits+1+fractionDigits)*11;
		spinner.setPreferredSize(d);
		spinner.setMaximumSize(d);
	}

	protected JCheckBox checkbox( String text , boolean value ) {
		return checkbox(text,value,null);
	}

	protected JCheckBox checkbox( String text , boolean value , String tooltip ) {
		JCheckBox c = new JCheckBox(text);
		c.setSelected(value);
		c.addActionListener( this);

		if( tooltip != null )
			c.setToolTipText(tooltip);

		return c;
	}

	public void addAlignLeft( JComponent target ) {
		addAlignLeft(target,this);
	}

	public void addAlignRight( JComponent target ) {
		addAlignRight(target,this);
	}

	public void addAlignCenter( JComponent target ) {
		addAlignCenter(target,this);
	}


	public static void addAlignLeft( JComponent target, JPanel owner ) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.add(target);
		p.add(Box.createHorizontalGlue());
		owner.add(p);
	}

	public static void addAlignRight( JComponent target, JPanel owner ) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.add(Box.createHorizontalGlue());
		p.add(target);
		owner.add(p);
	}

	public static void addAlignCenter( JComponent target, JPanel owner ) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.add(Box.createHorizontalGlue());
		p.add(target);
		p.add(Box.createHorizontalGlue());
		owner.add(p);
	}

	public void addCenterLabel( String text ) {
		addCenterLabel(text,this);
	}

	public void addCenterLabel( String text , JPanel owner ) {
		JLabel l = new JLabel(text);
		l.setAlignmentX(Component.CENTER_ALIGNMENT);
		owner.add(l);
		owner.add(Box.createRigidArea(new Dimension(1,8)));
	}

	public void addSeparator(int width) {
		add(Box.createRigidArea(new Dimension(1,8)));
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		separator.setMaximumSize(new Dimension(width, 5));
		add(separator);
		add(Box.createRigidArea(new Dimension(1, 8)));
	}

	public void addSeparator() {
		add(Box.createRigidArea(new Dimension(1, 8)));
		add(new JSeparator());
		add(Box.createRigidArea(new Dimension(1,8)));
	}

	public void addLabeled( JComponent target , String text ) {
		addLabeled(target,text,null);
	}

	public void addLabeled( JComponent target , String text , String tooltip ) {
		addLabeled(target,text, tooltip, this);
	}

	public static void addLabeled(JComponent target, String text, String tooltip, JPanel owner) {
		JLabel label = new JLabel(text);
		label.setLabelFor(target);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.add(label);
		p.add(Box.createHorizontalGlue());
		p.add(target);
		owner.add(p);

		if( tooltip != null ) {
			label.setToolTipText(tooltip);
		}
	}

	public static JPanel createHorizontalPanel( Component ...children ) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		for( var c : children ) {
			panel.add(c);
		}
		return panel;
	}

	public void addLabeledV( JComponent target , String text ) {
		addLabeledV(target,text,this);
	}

	public static void addLabeledV( JComponent target , String text , JPanel owner ) {
		JLabel label = new JLabel(text);
		label.setLabelFor(target);
		target.setAlignmentX(Component.CENTER_ALIGNMENT);
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.add(label);
		p.add(target);
		owner.add(p);
	}

	public void addVerticalGlue() {
		addVerticalGlue(this);
	}
	public static void addVerticalGlue( JPanel owner ) {
		owner.add(Box.createVerticalGlue());
	}

	public void addHorizontalGlue( JPanel owner ) {
		owner.add(Box.createHorizontalGlue());
	}

	protected void setEnabled( int index , boolean enabled ) {
		Component c = getComponent(index);
		if( c instanceof JPanel ) {
			JPanel p = (JPanel)c;
			p.getComponent(0).setEnabled(enabled);
			p.getComponent(2).setEnabled(enabled);
		} else if( c instanceof JLabel ) {
			c.setEnabled(enabled);
		}
	}

	/**
	 * Searches inside the children of "root" for a component that's a JPanel. Then inside the JPanel it
	 * looks for the target. If the target is inside the JPanel the JPanel is removed from root.
	 */
	protected static void removeChildInsidePanel( JComponent root , JComponent target ) {
		int N = root.getComponentCount();

		for (int i = 0; i < N; i++) {
			try {
				JPanel p = (JPanel)root.getComponent(i);
				Component[] children = p.getComponents();
				for (int j = 0; j < children.length; j++) {
					if( children[j] == target ) {
						root.remove(i);
						return;
					}
				}
			}catch( ClassCastException ignore){}
		}
	}

	/**
	 * Searches inside the children of "root" for 'target'. If found it is removed and the
	 * previous component.
	 */
	protected static void removeChildAndPrevious( JComponent root , JComponent target ) {
		int N = root.getComponentCount();

		for (int i = 0; i < N; i++) {
			if( root.getComponent(i) == target ) {
				root.remove(i);
				root.remove(i-1);
				return;
			}
		}
		throw new RuntimeException("Can't find component");
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		controlChanged(e.getSource());
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		controlChanged(e.getSource());
	}

	/**
	 * In almost all situations we just need to know that the state of a control has changed. No need to implement
	 * seperate listeners for all
	 */
	public void controlChanged( Object source ) {
		throw new RuntimeException("You need to override controlChanges() or implement actionPerformed()/stateChanged()");
	}
}
