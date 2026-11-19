#!/bin/sh
# ReadyGo run (macOS / Linux / Git Bash)
#   ./run.sh          Seoul (default region), KMA API
#   ./run.sh 부산     the given region
cd "$(dirname "$0")" || exit 1

mkdir -p out
javac -encoding UTF-8 --release 17 -d out src/readygo/*.java || exit 1
java -Dstdout.encoding=UTF-8 -cp out readygo.Main "$@"
