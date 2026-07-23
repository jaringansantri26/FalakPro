import pandas as pd
df = pd.read_excel('C:/FalakPro/Koordinat pantai barat amerika dan indonesia.xlsx', sheet_name='Amerika (237)')
for index, row in df.iterrows():
    lat = row["Lintang"]
    lon = row["Bujur"]
    print(f'        {lat} to {lon},')
