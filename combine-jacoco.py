import pandas as pd
import os

def get_jacoco_files(directory):
  ret = []
  for root, dirs, files in os.walk(directory):
    for file in files:
      fname=os.path.join(root, file)
      if file.endswith('jacoco.csv') and os.path.getsize(fname) > 10:
        ret.append(fname)
  return ret

files =  get_jacoco_files('.')
df=pd.DataFrame()

for f in files:
    print(f)
    x = pd.read_csv(f)
    x['jacoco_filename'] = f
    df = df.append(x)

df.to_csv('combined-jacoco.csv', index=False)
