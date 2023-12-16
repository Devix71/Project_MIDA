import os
from PIL import Image
import re

def convert_to_png_and_delete_old(directory, base_name="flag"):
    # Compile a regular expression pattern for matching the naming convention
    pattern = re.compile(f"{base_name}_(\d+)\.png")

    # Find the highest number already used
    max_number = 0
    for file in os.listdir(directory):
        match = pattern.match(file)
        if match:
            max_number = max(max_number, int(match.group(1)))

    # Start file numbering from the next number
    file_number = max_number + 1

    # List all files in the directory
    files = os.listdir(directory)

    for file in files:
        file_path = os.path.join(directory, file)

        # Skip files that already match the naming convention
        if pattern.match(file):
            continue

        # Check if the file is an image and not a directory or other file type
        if os.path.isfile(file_path):
            # Open the image
            with Image.open(file_path) as img:
                # Convert CMYK images to RGB
                if img.mode == 'CMYK':
                    img = img.convert('RGB')

                # Convert and save the image as PNG
                new_file_name = f"{base_name}_{file_number}.png"
                new_file_path = os.path.join(directory, new_file_name)
                img.save(new_file_path)
                
                print(f"Converted and saved {new_file_name}")

            # Delete the original file
            os.remove(file_path)
            print(f"Deleted original file: {file}")

            file_number += 1

# Usage
directory_path = r"C:\Users\Devix\OneDrive - University of Gothenburg\FurhatProject\dataset_flags"
convert_to_png_and_delete_old(directory_path)
