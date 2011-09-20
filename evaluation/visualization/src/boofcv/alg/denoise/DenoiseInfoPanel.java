package boofcv.alg.denoise;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Configures denoising algorithm and controls the GUI.
 *
 * @author Peter Abeles
 */
public class DenoiseInfoPanel extends JPanel implements ChangeListener , ActionListener {

	// selects which image to view
	JComboBox images;
	// specifies amount of noise to add to the image
	JSpinner noiseLevel;

	// A panel that just holds the config's position
	JPanel configHolder = new JPanel();

	// wavelet config
	JPanel waveletConfig;
	JComboBox waveletBox;
	JSpinner waveletLevel;

	// blur config
	JPanel blurConfig;
	JSpinner blurRadius;

	// Shows statistic info in error level
	JTextArea algError;
	JTextArea algErrorEdge;
	JTextArea noiseError;
	JTextArea noiseErrorEdge;

	// passes change events
	Listener listener;

	public DenoiseInfoPanel() {
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

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

		waveletLevel = new JSpinner(new SpinnerNumberModel(4,1,5,1));
		waveletLevel.addChangeListener(this);
		waveletLevel.setMaximumSize(new Dimension(75,(int)h));

		blurRadius = new JSpinner(new SpinnerNumberModel(1,1,12,1));
		blurRadius.addChangeListener(this);
		blurRadius.setMaximumSize(new Dimension(75,(int)h));

		algError = createErrorComponent();
		algErrorEdge = createErrorComponent();
		noiseError = createErrorComponent();
		noiseErrorEdge = createErrorComponent();

		configHolder.add(createWaveletConfig());
		createBlurConfig();

		addLabeled(images,"View",this);
		addLabeled(noiseLevel,"Noise",this);
		addSeparator();
		addCenterLabel("Denoised",this);
		addLabeled(algError,"Error",this);
		addLabeled(algErrorEdge,"Edge Error",this);
		addSeparator();
		addCenterLabel("Noise Image",this);
		addLabeled(noiseError,"Error",this);
		addLabeled(noiseErrorEdge,"Edge Error",this);
		addSeparator();
		add(configHolder);
		add(Box.createVerticalGlue());
	}

	private JPanel createWaveletConfig() {
		waveletConfig = new JPanel();
		waveletConfig.setLayout(new BoxLayout(waveletConfig,BoxLayout.Y_AXIS));
		addCenterLabel("Wavelet Config",waveletConfig);
		addLabeled(waveletBox,"Wavelet",waveletConfig);
		addLabeled(waveletLevel,"Level",waveletConfig);
		return waveletConfig;
	}

	private JPanel createBlurConfig() {
		blurConfig = new JPanel();
		blurConfig.setLayout(new BoxLayout(blurConfig,BoxLayout.Y_AXIS));
		addCenterLabel("Blur Config",blurConfig);
		addLabeled(blurRadius,"Radius",blurConfig);
		return blurConfig;
	}

	public void addCenterLabel( String text , JPanel owner ) {
		JLabel l = new JLabel(text);
		l.setAlignmentX(Component.CENTER_ALIGNMENT);
		owner.add(l);
		owner.add(Box.createRigidArea(new Dimension(1,8)));
	}

	public void addSeparator() {
		add(Box.createRigidArea(new Dimension(1,8)));
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		separator.setMaximumSize(new Dimension(200,5));
		add(separator);
		add(Box.createRigidArea(new Dimension(1,8)));
	}

	public void setWaveletActive( boolean active ) {
		if( active ){
			configHolder.remove(blurConfig);
			configHolder.add(waveletConfig);
		} else {
			configHolder.remove(waveletConfig);
			configHolder.add(blurConfig);
		}
		validate();
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

	private void addLabeled( JComponent target , String text , JPanel owner ) {
		JLabel label = new JLabel(text);
		label.setLabelFor(target);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.add(label);
		p.add(Box.createHorizontalGlue());
		p.add(target);
		owner.add(p);
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
		if( listener == null )
			return;

		if( e.getSource() == noiseLevel ) {
			listener.noiseChange(getNoiseSigma());
		} else if( e.getSource() == blurRadius ) {
			listener.noiseChange(getBlurRadius());
		} else if( e.getSource() == waveletLevel ) {
			listener.waveletChange(waveletBox.getSelectedIndex(),getWaveletLevel());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( listener == null )
			return;

		if( e.getSource() == images ) {
			listener.imageChange(images.getSelectedIndex());
		} else {
			listener.waveletChange(waveletBox.getSelectedIndex(),getWaveletLevel());
		}
	}

	public void setError(double algError, double algErrorEdge, double noiseError , double noiseErrorEdge) {
		this.algError.setText(String.format("%5.1f",algError));
		this.algErrorEdge.setText(String.format("%5.1f",algErrorEdge));
		this.noiseError.setText(String.format("%5.1f",noiseError));
		this.noiseErrorEdge.setText(String.format("%5.1f",noiseErrorEdge));
	}

	/**
	 * Returns the number of levels in the wavelet transform.
	 */
	public int getWaveletLevel() {
		return((Number)waveletLevel.getValue()).intValue();
	}

	/**
	 * Radius of the blur kernel
	 */
	public int getBlurRadius() {
		return ((Number)blurRadius.getValue()).intValue();
	}

	public float getNoiseSigma() {
		return ((Number)noiseLevel.getValue()).floatValue();
	}

	public static interface Listener
	{
		public void noiseChange( int radius );

		public void waveletChange( int which , int level );

		public void noiseChange( float sigma );

		public void imageChange( int which );
	}
}
