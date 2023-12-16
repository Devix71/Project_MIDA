import pandas as pd
import json
import os

# Define the path to the directory
directory_path = r"C:\Users\Devix\OneDrive - University of Gothenburg\FurhatProject"

# File names
input_file_name = 'flags_csv.csv'
output_file_name = 'flags_complete.csv'

# Full paths to the files
input_file_path = os.path.join(directory_path, input_file_name)
output_file_path = os.path.join(directory_path, output_file_name)

# Load the CSV file into a DataFrame
df = pd.read_csv(input_file_path)

# Update the 'object_type' in each row
for index, row in df.iterrows():
    # Parse the JSON in the 'region_attributes' column
    region_attributes = json.loads(row['region_attributes'])
    
    # Update the 'object_type'
    region_attributes['object_type'] = 'flag'

    # Convert the updated dictionary back to a JSON string and save it in the DataFrame
    df.at[index, 'region_attributes'] = json.dumps(region_attributes)

# Export the modified DataFrame to a new CSV file
df.to_csv(output_file_path, index=False)
