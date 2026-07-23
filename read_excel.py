import pandas as pd
file_path = "C:/FalakPro/Koordinat pantai barat amerika dan indonesia.xlsx"
xls = pd.ExcelFile(file_path)
for sheet_name in xls.sheet_names:
    print(f"\n--- Sheet: {sheet_name} ---")
    df = pd.read_excel(xls, sheet_name=sheet_name)
    print(df.to_string())
