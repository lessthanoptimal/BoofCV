`geo` is short for geometry and contains algorithms which deal with pure geometry. No image processing is allowed.
This is where 2D, 3D, and perspective geometry goes. Algorithms here tend to focus on low level operations and do
not combine information from multiple frames together explicitly.

Calibration is a bit of an exception and maybe it should be moved to `sfm`. It used to have its own package but
that stopped making sense.
