SRC = describe_opencv.cpp
TARG = describe_opencv
DETECT = detect_opencv


CC = g++

CPPFLAGS = -O3 -Wall -Wno-unused-function `pkg-config --cflags opencv`

LDFLAGS = `pkg-config --libs opencv`

OBJ = $(SRC:.cpp=.o)

all: $(TARG) $(DETECT) runtime

runtime: runtime_all_opencv.o
	$(CC) $(LDFLAGS) -o runtime runtime_all_opencv.o $(CPPFLAGS)

$(DETECT): detect_opencv.o
	$(CC) $(LDFLAGS) -o $(DETECT) detect_opencv.o $(CPPFLAGS)

clean: rm -f *~ *.o $(TARG) $(DETECT) runtime
