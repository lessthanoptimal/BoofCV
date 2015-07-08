import pyboof as pb

import numpy as np

original = pb.load_single_band('../../../data/applet/outdoors01.jpg',np.uint8)

# Let BoofCV decide on the type of image to store the gradient as
deriv_dtype = pb.gradient_dtype(pb.get_dtype(original))

# Declare the gradient images
derivX = pb.create_single_band(original.getWidth(),original.getHeight(),deriv_dtype)
derivY = pb.create_single_band(original.getWidth(),original.getHeight(),deriv_dtype)

# Compute the results for a few operators and visualize
pb.gradient(original,derivX,derivY,pb.GradientType.SOBEL)
buffered_sobel = pb.swing.colorize_gradient(derivX,derivY)

pb.gradient(original,derivX,derivY,pb.GradientType.PREWITT)
buffered_prewitt = pb.swing.colorize_gradient(derivX,derivY)

pb.gradient(original,derivX,derivY,pb.GradientType.THREE)
buffered_three = pb.swing.colorize_gradient(derivX,derivY)

pb.gradient(original,derivX,derivY,pb.GradientType.TWO0)
buffered_two0 = pb.swing.colorize_gradient(derivX,derivY)

pb.gradient(original,derivX,derivY,pb.GradientType.TWO1)
buffered_two1 = pb.swing.colorize_gradient(derivX,derivY)

# display the results in a single window as a list
image_list = [(original,"original"),
              (buffered_sobel,"sobel"),
              (buffered_prewitt,"prewitt"),
              (buffered_three,"three"),
              (buffered_two0,"two0"),
              (buffered_two1,"two1")]
pb.swing.show_list(image_list,title="Gradients")