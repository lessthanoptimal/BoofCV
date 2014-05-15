The point of the checks directory is so that modules which are independent of each other
can have their interactions tested in one location.  This was inspired by needing to
check to see if data structures in feature can be correctly serialized by IO.  However
IO and feature are completely independent of each other.