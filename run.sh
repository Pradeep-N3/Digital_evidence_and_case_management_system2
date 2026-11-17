#!/bin/sh
# Compiles and starts the Digital Evidence & Case Management System (Linux / macOS)
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -d out -sourcepath src src/dems/Main.java || { echo "Compilation failed. Install a JDK (javac -version)."; exit 1; }
java -cp out dems.Main
