#!/bin/sh
# ReadyGo automated tests (macOS / Linux / Git Bash). Results: tests/results/
cd "$(dirname "$0")/.." || exit 1

# Git Bash on Windows needs ';' as the classpath separator.
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
    *) SEP=':' ;;
esac

mkdir -p out
javac -encoding UTF-8 --release 17 -d out src/readygo/*.java || exit 1
javac -encoding UTF-8 --release 17 -cp out -d out/tests tests/ReadyGoTests.java tests/readygo/*.java || exit 1
java -Dstdout.encoding=UTF-8 -cp "out${SEP}out/tests" ReadyGoTests
