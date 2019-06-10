from merf import MERF
import re
import pandas as pd
import numpy as np


proj_path='.'

method=pd.read_csv(proj_path+'/src/main/java/method.csv',sep=';')
coverage=pd.read_csv(proj_path+'/src/test/java/coverageByMethod.csv',sep='|')

coverage['prodMethodName']=coverage.apply(lambda row: re.sub("(.*?)\\(.*", "\\1", row['prodMethod']), axis=1)
coverage['covratio']=coverage.apply(lambda row: row['coveredLines']/(row['coveredLines']+row['missedLines']), axis=1)

method['prodMethodName']=method.apply(lambda row: re.sub("(.*?)/.*", "\\1", row['method']), axis=1)

am=coverage.merge(method,left_on=['prodClassName','prodMethodName'], right_on=['class','prodMethodName'])

numerics = ['int16', 'int32', 'int64', 'float16', 'float32', 'float64']
x=am.select_dtypes(include=numerics)
x_cols=list(x.columns.values)
x_cols=list(set(x_cols)-set(['missedLines','coveredLines','covratio','line','startLine']))

train=am
mrf = MERF(n_estimators=100, max_iterations=20)
X_train =  train[x_cols]
Z_train = np.ones((len(X_train), 1))
print(Z_train.shape)
clusters_train = train['class']
y_train = train['covratio']

model=mrf.fit(X_train, Z_train, clusters_train, y_train)
print(model)
