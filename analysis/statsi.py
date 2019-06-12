from sklearn import tree
import statsmodels.api as sm
import statsmodels.formula.api as smf

import re
import pandas as pd
import numpy as np
from sklearn.metrics import mean_squared_error, f1_score

proj_path='.'

method=pd.read_csv(proj_path+'/src/main/java/method.csv',sep=';')
coverage=pd.read_csv(proj_path+'/src/test/java/coverageByMethod.csv',sep='|')

coverage['prodMethodName']=coverage.apply(lambda row: re.sub("(.*?)\\(.*", "\\1", row['prodMethod']), axis=1)
coverage['covratio']=coverage.apply(lambda row: row['coveredLines']/(row['coveredLines']+row['missedLines']), axis=1)

method['prodMethodName']=method.apply(lambda row: re.sub("(.*?)/.*", "\\1", row['method']), axis=1)

am=coverage.merge(method,left_on=['prodClassName','prodMethodName'], right_on=['class','prodMethodName'])

train=am

numerics = ['int16', 'int32', 'int64', 'float16', 'float32', 'float64']
x=am.select_dtypes(include=numerics)
x_cols=list(x.columns.values)
x_cols=list(set(x_cols)-set(['missedLines','coveredLines','covratio','line','startLine']))


clf = tree.DecisionTreeClassifier()
clf = clf.fit(train[x_cols], train['covratio'] > 0)

tree.export_graphviz(clf, out_file='/tmp/jfc_dt.dot', max_depth=4
   , feature_names=x_cols, class_names=['covered','uncovered']
   , filled = True)



md = smf.mixedlm("covratio ~ parameters+wmc+cbo+loc+returns+tryCatchQty+variables", train, groups=train["class"])
f=md.fit()
print(f.summary())
print('MSE:', mean_squared_error(f.predict(), train['covratio']))
print('F1: ', f1_score(f.predict() > 0, train['covratio'] > 0))

