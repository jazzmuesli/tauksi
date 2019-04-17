import pandas as pd
import os

def get_files(directory, mask):
  ret = []
  for root, dirs, files in os.walk(directory):
    for file in files:
      fname=os.path.join(root, file)
      if file.endswith(mask) and os.path.getsize(fname) > 10:
        ret.append(fname)
  return ret

def combine_files(mask, field_name, out_fname):
    files =  get_files('.', mask)
    df=pd.DataFrame()

    for f in files:
        print(f)
        x = pd.read_csv(f, header=None, error_bad_lines=False)
        x[field_name] = f
        df = df.append(x)

    df.to_csv(out_fname, index=False)


combine_files('jacoco.csv', 'jacoco_filename', 'combined-jacoco.csv')
combine_files('dependencies.txt', 'dep_filename', 'deps.csv')
combine_files('mutations.csv', 'mut_filename', 'pitest.csv')
