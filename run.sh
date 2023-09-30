#!/bin/bash

rm -rf ./generated/cpu/*
# generate verilog file
sbt "runMain rv32e.top_main"

# Define the file name
file_name="generated/cpu/top.v"

# Define the comments to add
comments="/* verilator lint_off DECLFILENAME */\n/* verilator lint_off UNUSEDSIGNAL */\n/* verilator lint_off UNDRIVEN */\n/* verilator lint_off UNOPTFLAT */\n/* verilator lint_off WIDTHEXPAND */"

# Check if the file exists
if [ -e "$file_name" ]; then
  # Add comments to the beginning of the file
  sed -i "1i $comments" "$file_name"
  echo "Comments added to $file_name"
else
  echo "File $file_name does not exist."
fi

# Define the source directory
source_dir="/home/zhuyangyang/project/nju_digital_design_chisel/chisel-empty/generated/cpu"

# Define the destination directory
destination_dir="/home/zhuyangyang/project/ysyx-workbench/npc/vsrc"


# Check if the source directory exists
if [ -d "$source_dir" ]; then
  # Create the destination directory if it doesn't exist
  mkdir -p "$destination_dir"

  # Search for .v files in the source directory and copy them to the destination directory
  find "$source_dir" -type f -name "*.v" -exec cp {} "$destination_dir" \;

  echo "Copied .v files from $source_dir to $destination_dir"
else
  echo "Source directory $source_dir does not exist."
fi

# cp /home/zhuyangyang/project/nju_digital_design_chisel/chisel-empty/generated/top.v /home/zhuyangyang/project/ysyx-workbench/npc/vsrc