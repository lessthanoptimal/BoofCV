# Indicates compilator to use
CC      = g++

# Specifies compilator options
CFLAGS  = -O3 -m32 -Wall -ISURF-V1.0.9
LDFLAGS = 
LDLIBS  = -m32 SURF-V1.0.9/libSurf.a

# Files extensions .cpp, .o
SUFFIXES = .cpp .o 
.SUFFIXES: $(SUFFIXES) .

# Name of the main program
PROG  = describe_surf

# Object files .o necessary to build the main program
OBJS  = describe_surf.o SURF-V1.0.9/imload.o

all: $(PROG)

# Compilation and link
$(PROG): $(OBJS)
	$(CC) $(LDFLAGS) -o $(PROG) $(OBJS) $(LDLIBS) 

%.o: %.cpp
	$(CC)  $(CFLAGS) -c $< -o $@

clean:
	-rm -f $(PROG)
	-rm -f *.o
