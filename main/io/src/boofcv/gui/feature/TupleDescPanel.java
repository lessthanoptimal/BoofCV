package boofcv.gui.feature;

import boofcv.struct.feature.TupleDesc_F64;

import javax.swing.*;
import java.awt.*;


/**
 * Visualizes the a {@link boofcv.struct.feature.TupleDesc_F64}.
 *
 * @author Peter Abeles
 */
public class TupleDescPanel extends JPanel {

	TupleDesc_F64 desc;

	public TupleDesc_F64 getDesc() {
		return desc;
	}

	public void setDescription(TupleDesc_F64 desc) {
		this.desc = desc;
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;

		TupleDesc_F64 desc = this.desc;
		if( desc == null ) {
			g2.setColor(Color.WHITE);
			g2.fillRect(0,0,getWidth(),getHeight());
		} else {

			int h = getHeight();
			int w = getWidth();

			int m = h/2;

			int []x = new int[ desc.value.length ];
			int []y = new int[ desc.value.length ];

			// find the maximum magnitude of any of the elements
			double max = 0;
			for( double d : desc.value ) {
				if( max < Math.abs(d)) {
					max = Math.abs(d);
				}
			}

			// draw a normalized histogram plot
			double stepX = 1.0/desc.value.length;

			for( int i = 0; i < desc.value.length; i++ ) {
				x[i] = (int)(w*i*stepX);
				y[i] = (int)((m*desc.value[i]/max)+m);
			}

			g2.setColor(Color.GRAY);
			g2.drawLine(0,m,w,m);

			g2.setStroke(new BasicStroke(2));
			g2.setColor(Color.RED);
			g2.drawPolyline(x,y,x.length);

			// print out the magnitude
			g2.setColor(Color.BLACK);
			String s = String.format("%4.1e",max);
			g2.drawString(s,0,20);
		}
	}
}
