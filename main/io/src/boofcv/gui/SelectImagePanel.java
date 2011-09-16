package boofcv.gui;

import boofcv.gui.image.ImagePanel;
import boofcv.io.image.ImageListManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * Panel where a toolbar is provided for selecting the input image
 *
 * @author Peter Abeles
 */
public abstract class SelectImagePanel extends JPanel implements ActionListener {
	JToolBar toolbar;
	JComboBox imageBox;
	JCheckBox originalCheck;
	ImageListManager imageManager;

	protected BufferedImage inputImage;

	// panel used for displaying the original image
	protected ImagePanel origPanel = new ImagePanel();
	// the main GUI being displayed
	protected Component gui;

	public SelectImagePanel() {
		super(new BorderLayout());
		toolbar = new JToolBar();

		imageBox = new JComboBox();
		toolbar.add(imageBox);
		imageBox.addActionListener(this);
		originalCheck = new JCheckBox("Show Input");
		toolbar.add(originalCheck);
		originalCheck.addActionListener(this);

		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.PAGE_START);
	}

	public void setMainGUI( final Component gui ) {
		this.gui = gui;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				add(gui,BorderLayout.CENTER);
			}});
	}

	public void setInputImage( BufferedImage image ) {
		inputImage = image;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if( inputImage == null ) {
					originalCheck.setEnabled(false);
				} else {
					originalCheck.setEnabled(true);
					origPanel.setBufferedImage(inputImage);
					origPanel.setPreferredSize(new Dimension(inputImage.getWidth(),inputImage.getHeight()));
					origPanel.repaint();
				}
			}});
	}


	public void setImageManager( final ImageListManager manager ) {
		this.imageManager = manager;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for( int i = 0; i < manager.size(); i++ ) {
					imageBox.addItem(manager.getLabel(i));
				}
			}});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == imageBox ) {
			final String name = (String)imageBox.getSelectedItem();
			new Thread() {
				public void run() {
					performChangeImage(name, imageBox.getSelectedIndex());
				}
			}.start();
		} else {
			origPanel.setSize(gui.getWidth(),gui.getHeight());
			// swap the main GUI with a picture of the original input image
			if( originalCheck.isSelected() ) {
				remove(gui);
				add(origPanel);
			} else {
				remove(origPanel);
				add(gui);
			}
			validate();
			repaint();
		}
	}

	private void performChangeImage(String name , int index ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				toolbar.setEnabled(false);
			}});
		changeImage( name , index );
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				toolbar.setEnabled(true);
			}});
	}

	public ImageListManager getImageManager() {
		return imageManager;
	}

	public abstract void changeImage( String name , int index );
}
