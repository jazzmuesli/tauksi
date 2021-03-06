import pandas as pd
deps=pd.read_csv('deps.csv',sep=";")
deps['project']=deps.apply(lambda row:row['dep_filename'].replace('./','').replace('/dependencies.txt',''), axis=1)
# strip to one project
deps=deps[deps['project']=='commons-math']

# create a dictionary from from_class, to_class, project to ID
u=deps['0'].append(deps['2']).append(deps['project']).unique()
d=dict(zip(u, range(1,len(u)+1)))
df=pd.DataFrame({"id": list(d.values()), "value":list(d.keys())})
df.to_csv('tauksi.dict',header=False,index=False,sep="\t")

def attach_id(field,fid):
    deps[fid]=deps.apply(lambda row: d[row[field]], axis=1).astype('int')
attach_id('0','from_id')
attach_id('2','to_id')
attach_id('project','project_id')
edges=deps[deps['1']=='create'][['from_id','to_id']]
pedges=deps[['from_id','project_id']].drop_duplicates()
pedges=pedges.rename(index=str, columns={"project_id":"to_id"})
edges=edges.append(pedges,sort=True)
edges.to_csv('tauksi.cites',sep="\t",index=False,header=False)

metrics=pd.read_csv('class-metrics.csv',sep=';')
features=metrics.merge(deps[['0','from_id']].drop_duplicates(),how='inner',left_on='class',right_on='0')
col_metrics=[x for x in metrics.columns if not x in ['file','class','type']][:1]
features=features[['from_id']+col_metrics+['type']]
for c in col_metrics:
    features[c] = features.apply(lambda row: 1 if row[c] > 0 else 0, axis=1).astype('int')
features.to_csv('tauksi.content',sep="\t",index=False,header=False)
