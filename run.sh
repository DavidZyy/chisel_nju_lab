#!/bin/bash

# Define the file name
file_name="generated/top.v"

# Define the comments to add
comments="/* verilator lint_off DECLFILENAME */\n/* verilator lint_off UNUSEDSIGNAL */\n/* verilator lint_off UNDRIVEN */"

# Check if the file exists
if [ -e "$file_name" ]; then
  # Add comments to the beginning of the file
  sed -i "1i $comments" "$file_name"
  echo "Comments added to $file_name"
else
  echo "File $file_name does not exist."
fi

cp /home/zhuyangyang/project/nju_digital_design_chisel/chisel-empty/generated/top.v /home/zhuyangyang/project/ysyx-workbench/npc/vsrc