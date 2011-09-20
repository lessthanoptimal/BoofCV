package boofcv.alg.denoise;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * @author Peter Abeles
 */
public class DenoiseInfoPanel extends JPanel implements ChangeListener , ActionListener {

	JComboBox images;
	JComboBox waveletBox;
	JSpinner noiseLevel;

	JTextArea algError;
	JTextArea algErrorEdge;
	JTextArea noiseError;
	JTextArea noiseErrorEdge;

	Listener listener;

	public DenoiseInfoPanel() {
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

		images = new JComboBox();
		images.addItem("Denoised");
		images.addItem("Noisy");
		images.addItem("Original");
		images.addActionListener(this);
		images.setMaximumSize(images.getPreferredSize());

		waveletBox = new JComboBox();
		waveletBox.addActionListener(this);
		double h = waveletBox.getPreferredSize().getHeight();
		waveletBox.setMaximumSize(new Dimension(175,(int)h));

		noiseLevel = new JSpinner(new SpinnerNumberModel(20,0,100,5));
		noiseLevel.addChangeListener(this);
		noiseLevel.setMaximumSize(noiseLevel.getPreferredSize());


		algError = createErrorComponent();
		algErrorEdge = createErrorComponent();
		noiseError = createErrorComponent();
		noiseErrorEdge = createErrorComponent();

		addLabeled(images,"View");
		addLabeled(noiseLevel,"Noise");
		addSeparator();
		addCenterLabel("Denoised");
		addLabeled(algError,"Error");
		addLabeled(algErrorEdge,"Edge Error");
		addSeparator();
		addCenterLabel("Noise Image");
		addLabeled(noiseError,"Error");
		addLabeled(noiseErrorEdge,"Edge Error");
		addSeparator();
		addCenterLabel("Wavelet Config");
		addLabeled(waveletBox,"Wavelet");
		add(Box.createVerticalGlue());
	}

	public void addCenterLabel( String text ) {
		JLabel l = new JLabel(text);
		l.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(l);
		add(Box.createRigidArea(new Dimension(1,8)));
	}

	public void addSeparator() {
		add(Box.createRigidArea(new Dimension(1,8)));
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		separator.setMaximumSize(new Dimension(200,5));
		add(separator);
		add(Box.createRigidArea(new Dimension(1,8)));
	}

	public void setWaveletActive( boolean active ) {
		waveletBox.setEnabled(active);
		waveletBox.repaint();
	}

	public void addWaveletName( String name ) {
		waveletBox.addItem(name);
		waveletBox.validate();
	}

	private JTextArea createErrorComponent() {
		JTextArea comp = new JTextArea(1,6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		return comp;
	}

	private void addLabeled( JComponent target , String text ) {
		JLabel label = new JLabel(text);
		label.setLabelFor(target);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.add(label);
		p.add(Box.createHorizontalGlue());
		p.add(target);
		add(p);
	}

	public void reset() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				images.setSelectedIndex(0);
				noiseLevel.setValue(20);
			}});
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		float sigma = ((Number)((JSpinner)e.getSource()).getValue()).floatValue();
		if( listener != null )
			listener.noiseChange(sigma);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == images ) {
			if( listener != null )
				listener.imageChange(images.getSelectedIndex());
		} else {
			if( listener != null )
				listener.waveletChange(waveletBox.getSelectedIndex());
		}
	}

	public void setError(double algError, double algErrorEdge, double noiseError , double noiseErrorEdge) {
		this.algError.setText(String.format("%5.2f",algError));
		this.algErrorEdge.setText(String.format("%5.2f",algErrorEdge));
		this.noiseError.setText(String.format("%5.2f",noiseError));
		this.noiseErrorEdge.setText(String.format("%5.2f",noiseErrorEdge));
	}

	public static interface Listener
	{
		public void waveletChange( int which );

		public void noiseChange( float sigma );

		public void imageChange( int which );
	}
}
