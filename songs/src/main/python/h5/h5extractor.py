import h5py
import os
import h5py
import os
import json
import numpy as np

class NumpyEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, np.integer):
            return int(obj)
        elif isinstance(obj, np.floating):
            return float(obj)
        elif isinstance(obj, np.ndarray):
            return obj.tolist()
        else:
            return super(NumpyEncoder, self).default(obj)


directory = '/Users/duvalle/Documents/GitHub/dl4s/data/Songs/MillionSongSubset'
collected_data = []
for root, dirs, files in os.walk(directory):
    for filename in files:
        if filename.endswith(".h5"):
            file_path = os.path.join(root, filename)
            f = h5py.File(file_path, 'r')
            # print(list(f.keys()))
            data = f['metadata']
            # print(data)
            # print("KEYS:  ", data.keys())
            ds = data['songs']
            # print(ds)
            # print("SHAPE:  ", ds.shape)
            # print("TYPE:  ", ds.dtype)
            # print("-------")
            for entry in ds:
                entry_data = {
                      'artist': entry["artist_name"].decode("utf-8"),
                      'song': entry["title"].decode("utf-8"),
                    #   'artist_familiarity': entry["artist_familiarity"],
                    #   'artist_hotttnesss': entry["artist_hotttnesss"],
                      'artist_id': entry["artist_id"].decode("utf-8"),
                      'genre': entry["genre"].decode("utf-8"),
                      'idx_similar_artists': entry["idx_similar_artists"],
                      'release': entry["release"].decode("utf-8"),
                    #   'song_hotttnesss': entry["song_hotttnesss"],
                      'song_id': entry["song_id"].decode("utf-8"),
                      }
                if not np.isnan(entry["song_hotttnesss"]):
                    entry_data['song_hotttnesss'] = entry["song_hotttnesss"]
                collected_data.append(entry_data)
                if not np.isnan(entry["artist_familiarity"]):
                    entry_data['artist_familiarity'] = entry["artist_familiarity"]
                if not np.isnan(entry["artist_hotttnesss"]):
                    entry_data['artist_hotttnesss'] = entry["artist_hotttnesss"]
            f.close()

# Specify the path to the JSON file
json_file = '/Users/duvalle/Documents/GitHub/dl4s/songs/src/main/python/collected_data.json'

# Write the collected_data list to the JSON file
with open(json_file, 'w') as f:
    json.dump(collected_data, f, cls=NumpyEncoder)