import cv2
import pyboof as pb
import numpy as np

cap = cv2.VideoCapture(0)

refPt=[(0,0),(0,0)]
state = 0
quad = pb.Quadrilateral2D()

cv2.namedWindow("frame")

def click_and_crop(event, x, y, flags, param):
    # grab references to the global variables
    global refPt, state

    # if the left mouse button was clicked, record the starting
    # (x, y) coordinates and indicate that cropping is being
    # performed
    if event == cv2.EVENT_LBUTTONDOWN:
        refPt = [(x, y),(x,y)]
        state = 1
    elif event == 0:
        refPt[1] = (x,y)
    elif event == cv2.EVENT_LBUTTONUP:
        state = 2

def pts_to_quad():
    global refPt, quad
    quad.a.set((refPt[0][0],refPt[0][1]))
    quad.b.set((refPt[1][0],refPt[0][1]))
    quad.c.set((refPt[1][0],refPt[1][1]))
    quad.d.set((refPt[0][0],refPt[1][1]))



cv2.setMouseCallback("frame", click_and_crop)

# tracker = pb.FactoryTrackerObjectQuad(np.uint8).circulant()
tracker = pb.FactoryTrackerObjectQuad(np.uint8).tld()

while True:
    # Capture frame-by-frame
    ret, frame = cap.read()

    if not ret:
        print "Failed to read frame"
        exit(-1)

    # Our operations on the frame come here
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    boof_gray = pb.ndarray_to_boof(gray)

    if state == 1:
        cv2.rectangle(gray, refPt[0], refPt[1], (0, 255, 0), 2)
    elif state == 2:
        pts_to_quad()
        if not tracker.initialize(boof_gray,quad):
            print "Initialization failed!"
            state = 0
        else:
            print "Success"
            state = 3
    elif state == 3:
        if not tracker.process(boof_gray,quad):
            print "Tracking failed!"
        else:
            lines =  np.array(quad.get_tuple_tuple())
            cv2.polylines(gray,np.int32([lines]),True,(255, 0, 0))

    # Display the resulting frame
    cv2.imshow('frame',gray)
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# When everything done, release the capture
cap.release()
cv2.destroyAllWindows()