# Variables
JAVAC = javac
JAVA  = java
SRC_DIR = src
BIN_DIR = bin
MAIN_CLASS = com.mmahlatji.flipwatersim.Main

# Default target: compile all Java files
all:
	@mkdir -p $(BIN_DIR)
	$(JAVAC) -d $(BIN_DIR) $$(find $(SRC_DIR) -name "*.java")

# Run the application
run: all
	$(JAVA) -cp $(BIN_DIR) $(MAIN_CLASS)

# Clean up build artifacts
clean:
	rm -rf $(BIN_DIR)