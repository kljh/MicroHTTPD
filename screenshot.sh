#!/bin/bash

adb=~/Library/Android/sdk/platform-tools/adb

$adb shell screencap -p /sdcard/screenshot.png
$adb pull /sdcard/screenshot.png screenshot.png
