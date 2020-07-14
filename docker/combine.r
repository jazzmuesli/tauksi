#!/usr/bin/env Rscript
#/Library/Frameworks/R.framework/Versions/3.5/Resources/bin/Rscript 


args=commandArgs(trailing=T)
out_fname=ifelse(length(args)==1, args[1], "combined-metrics.csv")
library(plyr)
library(data.table)
input<-file('stdin', 'r')
files <- readLines(input)
data=data.frame()
for (f in files) {
	x=data.table::fread(file=f, sep=";")
	x$filename=f
	data=rbind.fill(data, x)
}
print(out_fname)
write.table(data, out_fname, row.names=F, sep=";")
