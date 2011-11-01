SRC = main.cpp
TARG = main.o runtime


CC = g++

CPPFLAGS = -O3 -Wall -Wno-unused-function `pkg-config --cflags opencv`

LDFLAGS = `pkg-config --libs opencv`

OBJ = $(SRC:.cpp=.o)

all: $(TARG)

runtime: main.o
	$(CC) $(LDFLAGS) -o runtime main.o $(CPPFLAGS)

clean: rm -f *~ *.o $(TARG) runtime
