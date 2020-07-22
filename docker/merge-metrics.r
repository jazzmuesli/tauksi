library(data.table)
library(dplyr)
library(plyr)
nao=function(x) ifelse(length(x) > 1, x[!is.na(x)][1], x)
files=list.files(path=".", pattern="metrics.csv", recursive = T, full.names = T)
ret=data.frame()
for (f in files) {
  ret=rbind.fill(ret, fread(f,sep=";"))
}

merged=ret %>% group_by(prodClassName) %>% summarise_all(list(nao)) %>% as.data.frame()
write.table(merged,"target/metrics.csv", sep=";", row.names=F)

