#!/bin/bash

set -eo pipefail

echo "Executing tasks: [monkey runner]"


adb shell "monkey -p com.scape.pixscape -v 1000"