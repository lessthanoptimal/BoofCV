# Jacobian

* Added analytic jacobian for universal omni

# Regularization

The optimization step might benefit from regularization to prevent rediculous solutions from being generated.
Primarily a concern only when there's on insufficient number of images.

# Parameter Hinting

The non-linear step could be provided a hint for initial parameter values like some papers suggest. Tested idea
on a few data sets and didn't seem to change the end result.

# Fisheye Calibration

Chessboard detector doesn't detect exact corners towards image border

* Assumes straight lines when they are curved
* Solutions
  * Going back to or providing the option of using a corner based detector
  * Fit quadratic edges instead of straight lines
  * Hybrid approach. Detect squares, compute corners using a corner detector