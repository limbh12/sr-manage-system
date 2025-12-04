import pandas as pd

try:
    file_path = 'docs/openapi_survey.xlsx'
    # Read the Excel file
    df = pd.read_excel(file_path)
    
    print("Columns in Excel file:")
    for col in df.columns:
        print(col)
        
    print("\nFirst few rows:")
    print(df.head().to_string())
    
except Exception as e:
    print(f"Error reading Excel file: {e}")
