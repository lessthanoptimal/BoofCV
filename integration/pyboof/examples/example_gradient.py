import pyboof.ip as ip
import pyboof.image as image
import pyboof.swing as swing

import numpy as np

original = image.load_single_band('../../../data/applet/outdoors01.jpg',np.uint8)

# Let BoofCV decide on the type of image to store the gradient as
deriv_dtype = image.gradient_dtype(image.get_dtype(original))

# Declare the gradient images
derivX = image.create_single_band(original.getWidth(),original.getHeight(),deriv_dtype)
derivY = image.create_single_band(original.getWidth(),original.getHeight(),deriv_dtype)

# Compute the results for a few operators and visualize
ip.gradient(original,derivX,derivY,ip.GradientType.SOBEL)
buffered_sobel = swing.colorize_gradient(derivX,derivY)

ip.gradient(original,derivX,derivY,ip.GradientType.PREWITT)
buffered_prewitt = swing.colorize_gradient(derivX,derivY)

ip.gradient(original,derivX,derivY,ip.GradientType.THREE)
buffered_three = swing.colorize_gradient(derivX,derivY)

ip.gradient(original,derivX,derivY,ip.GradientType.TWO0)
buffered_two0 = swing.colorize_gradient(derivX,derivY)

ip.gradient(original,derivX,derivY,ip.GradientType.TWO1)
buffered_two1 = swing.colorize_gradient(derivX,derivY)

# display the results in a single window as a list
image_list = [(original,"original"),
              (buffered_sobel,"sobel"),
              (buffered_prewitt,"prewitt"),
              (buffered_three,"three"),
              (buffered_two0,"two0"),
              (buffered_two1,"two1")]
swing.show_list(image_list,title="Gradients")