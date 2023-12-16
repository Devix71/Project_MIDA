import pandas as pd

# Replace 'your_file.csv' with the path to your CSV file
file_path = r"C:\Users\Devix\OneDrive - University of Gothenburg\FurhatProject\flags_complete.csv"

try:
    # Read the CSV file
    df = pd.read_csv(file_path)

    # Display the first few rows of the DataFrame
    print(df.head())
except Exception as e:
    print(f"An error occurred: {e}")
