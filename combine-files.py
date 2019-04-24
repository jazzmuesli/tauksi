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

def combine_files(mask, field_name, out_fname,h=None,sep=','):
    files =  get_files('.', mask)
    df=pd.DataFrame()

    for f in files:
        print(f)
        x = pd.read_csv(f, header=h, error_bad_lines=False,sep=sep)
        x[field_name] = f
        df = df.append(x)

    df.to_csv(out_fname, index=False,sep=';')


combine_files('jacoco.csv', 'jacoco_filename', 'combined-jacoco.csv',0,',')
combine_files('dependencies.txt', 'dep_filename', 'deps.csv')
combine_files('mutations.csv', 'mut_filename', 'pitest.csv')
combine_files('git-history.csv', 'gith_filename', 'ghistory.csv',0,';')
