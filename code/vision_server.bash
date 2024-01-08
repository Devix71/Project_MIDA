#!/bin/bash

# Set the PATH variable, appending the Miniconda bin directory
export PATH="/path/to/miniconda/library/bin:$PATH"

# Change to the specified directory
cd "/path/to/furhat_skill"

# Activate the Conda environment
source activate <YOUR ENVIRONMENT NAME>

# Run the Python script
python object-detection-server/objserv.py