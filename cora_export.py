import pandas as pd
deps=pd.read_csv('deps.csv',sep=";")
deps['project']=deps.apply(lambda row:row['dep_filename'].replace('./','').replace('/dependencies.txt',''), axis=1)
# create a dictionary from from_class, to_class, project to ID
u=deps['0'].append(deps['2']).append(deps['project']).unique()
d=dict(zip(u, range(1,len(u)+1)))
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
features=features[['from_id']+[x for x in metrics.columns if not x in ['file','class','type']]+['type']]
features.to_csv('tauksi.content',sep="\t",index=False,header=False)
