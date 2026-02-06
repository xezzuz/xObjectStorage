JAVAC = javac
JAVA  = java

SRC_DIR = src
BIN_DIR = bin

SOURCES := $(shell find $(SRC_DIR) -name "*.java")

all: compile

compile:
	@mkdir -p $(BIN_DIR)
	$(JAVAC) -d $(BIN_DIR) $(SOURCES)

run: compile
	$(JAVA) -cp $(BIN_DIR) cli.Main

clean:
	rm -rf $(BIN_DIR)

re: clean all

.PHONY: all compile run clean re
