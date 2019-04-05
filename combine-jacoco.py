import pandas as pd
import os

def get_jacoco_files(directory):
  ret = []
  for root, dirs, files in os.walk(directory):
    for file in files:
      if file.endswith('jacoco.csv'):
        ret.append(os.path.join(root, file))
  return ret

files =  get_jacoco_files('.')
df=pd.DataFrame()

for f in files:
    df = df.append(pd.read_csv(f))

df.to_csv('combined-jacoco.csv', index=False)

