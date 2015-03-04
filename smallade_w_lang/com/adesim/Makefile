# Recursive ade2010 Makefile -- Jack Harris

#CP="core/ADEcore.jar:.:jars/visionJNI.jar"
NAMESPACE="com/adesim/"
default: all

all:
	@for d in  $(shell find . -maxdepth 1 -type d ! \( -name ".svn" \) ! \( -name "config" \) ! \( -name "assignments" \) |cut -d '/' -f2); \
    do \
	set -e; \
        cd ../..>/dev/null; \
        echo "javac -cp ${CP} ${NAMESPACE}$$d/*.java"; \
        javac -cp ${CP} ${NAMESPACE}$$d/*.java; \
        cd ->/dev/null; \
    done;
	@for dd in  $(shell find assignments -maxdepth 1 -type d ! \( -name "." \) ! \( -name ".svn" \) ! \( -name "assignments" \) |cut -d '/' -f2); \
    do \
	set -e; \
        cd ../..>/dev/null; \
        echo "javac -cp ${CP} ${NAMESPACE}assignments/$$dd/*.java"; \
        javac -cp ${CP} ${NAMESPACE}assignments/$$dd/*.java; \
        cd ->/dev/null; \
    done;

clean:
	@for d in  $(shell find . -maxdepth 1 -type d ! \( -name ".svn" \) ! \( -name "config" \) ! \( -name "assignments" \) |cut -d '/' -f2); \
    do \
        rm -f $$d/*.class; \
    done;
	@for dd in  $(shell find assignments -maxdepth 1 -type d ! \( -name "." \) ! \( -name ".svn" \) ! \( -name "assignments" \) |cut -d '/' -f2); \
    do \
        rm -f assignments/$$dd/*.class; \
    done;


help:
	@echo  javac -cp ${CP} for all sub directories 
	@for d in  $(shell find . -maxdepth 1 -type d ! \( -name ".svn" \) ! \( -name "config" \) ! \( -name "assignments" \)|cut -d '/' -f2); do echo "  $$d"; done;
	@for d in  $(shell find assignments -maxdepth 1 -type d ! \( -name ".svn" \)  ! \( -name "assignments" \) |cut -d '/' -f2); do echo "  assignments/$$d"; done;

.PHONY: help
