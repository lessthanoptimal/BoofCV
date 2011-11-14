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
SDETECT = detect_surf
RUNTIME = runtime_desc_surf
DETECT = runtime_detect_surf
RALL = runtime_all_surf

# Object files .o necessary to build the main program
OBJS  = describe_surf.o  SURF-V1.0.9/imload.o
SDOBJS  = detect_surf.o  SURF-V1.0.9/imload.o
ROBJS = runtime_surf.o SURF-V1.0.9/imload.o
DOBJS = runtime_detect_surf.o SURF-V1.0.9/imload.o
AOBJS = runtime_all_surf.o SURF-V1.0.9/imload.o

all: $(PROG) $(SDETECT) $(RUNTIME) $(DETECT) $(RALL)

# Compilation and link
$(PROG): $(OBJS)
	$(CC) $(LDFLAGS) -o $(PROG) $(OBJS) $(LDLIBS) 
$(SDETECT): $(SDOBJS)
	$(CC) $(LDFLAGS) -o $(SDETECT) $(SDOBJS) $(LDLIBS) 
$(RUNTIME): $(ROBJS)
	$(CC) $(LDFLAGS) -o $(RUNTIME) $(ROBJS) $(LDLIBS) 
$(DETECT): $(DOBJS)
	$(CC) $(LDFLAGS) -o $(DETECT) $(DOBJS) $(LDLIBS) 
$(RALL): $(AOBJS)
	$(CC) $(LDFLAGS) -o $(RALL) $(AOBJS) $(LDLIBS) 

%.o: %.cpp
	$(CC)  $(CFLAGS) -c $< -o $@

clean:
	-rm -f $(PROG) $(SDETECT) $(RUNTIME) $(DETECT) $(RALL)
	-rm -f *.o
