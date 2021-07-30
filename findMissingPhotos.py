#!/usr/bin/python3
#
# Bahnhofsfotos Fotoverfügbarkeit Test
# Pieter Hollants <pieter@hollants.com>
# Peter Storch
#
# Aufruf:
#   findMissingPhotos.py missing.csv
#

import csv, requests, sys, os.path
from urllib.parse import unquote

print(f"Hole alle Bahnhöfe von der API...", file=sys.stderr)

r = requests.get(f"https://api.railway-stations.org/stations?hasPhoto=true")
r.raise_for_status()

stations = sorted(r.json(), key=lambda item: item['country'] + ' ' + item['idStr'] + ' ' + item['title'])
num_stations = len(stations)

csvwriter = csv.writer(sys.stdout)

for current_station, station in enumerate(stations, start=1):
	print(f"\rPrüfe Fotoverfügbarkeit für Bahnhof {current_station}/{num_stations}...", end="", file=sys.stderr, flush=True)
	if station["photoUrl"]:
            photoFile = unquote(station["photoUrl"]).replace("https://api.railway-stations.org/photos","/var/www/drupal/fotos")
            if not os.path.exists(photoFile):
                csvwriter.writerow((station["country"], station["idStr"], station["title"], "https://map.railway-stations.org/station.php?countryCode=" + station["country"] + "&stationId=" + station["idStr"], station["photoUrl"]))

