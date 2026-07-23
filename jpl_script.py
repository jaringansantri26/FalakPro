import urllib.request
import json

def get_horizons_data(target, time):
    # target: '10' for Sun, '301' for Moon
    url = f"https://ssd.jpl.nasa.gov/api/horizons.api?format=text&COMMAND='{target}'&OBJ_DATA='NO'&MAKE_EPHEM='YES'&EPHEM_TYPE='OBSERVER'&CENTER='500@399'&START_TIME='{time}'&STOP_TIME='{time}'&STEP_SIZE='1m'&QUANTITIES='1,2,31'"
    url = url.replace(" ", "%20")
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req) as response:
        return response.read().decode('utf-8')

print("Fetching Sun data...")
sun_data = get_horizons_data('10', '2026-06-15 10:44:30')
print("Fetching Moon data...")
moon_data = get_horizons_data('301', '2026-06-15 10:44:30')

with open('jpl_sun.txt', 'w') as f:
    f.write(sun_data)
with open('jpl_moon.txt', 'w') as f:
    f.write(moon_data)

print("Done")
