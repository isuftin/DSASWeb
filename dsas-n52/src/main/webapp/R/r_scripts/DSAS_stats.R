# wps.des: id=DSAS_stats, title=Digital Shoreline Analysis System Stats, abstract=stats available - LRR LCI WLR WCI SCE NSM EPR ECI;
# wps.in: input, xml, block intersection text, text input from intersections layer with time elements and uncertainty;
# wps.in: ci, double, confidence interval, percentage for confidence level > 0.5 and < 1.0;
# input is unique identifier for WPS, is a variable in R (will contain all parser text)
# xml is for WPS side of things, tells WPS how input should be formatted

numPar = 4 # number of cores
num_c = 5 # num of cols in file

localRun <- FALSE

if (localRun){
  Rprof("DSAS_profiler.txt")
  ci <- 0.95
  input <- "testOut.tsv"
  ptm <- proc.time() # for time of process
}

if (ci>=1 || ci<=0.5){
  stop("confidence interval argument must be between 0.5 and 1.0 (non-inclusive)")
}

statLongNames  <-  data.frame("LRR"="Linear regression rate",
                         "LCI"="Linear regression rate CI",
                         "WLR"="Weighted linear regression rate",
                         "WCI"="Weighted linear regression rate CI",
                         "SCE"="Shoreline change envelope",
                         "NSM"="Net shoreline movement",
                         "EPR"="End point rate",
                         "ECI"="End point rate uncertainty")
statUnits <-  data.frame("LRR"="m yr^-1","LCI"="m yr^-1","WLR"="m yr^-1",
                         "WCI"="m yr^-1","SCE"="m","NSM"="m","EPR"="m yr^-1","ECI"="m yr^-1")
fileN    <- input # will have input as a string (long string read in)
conLevel <- ci
zRepV    <- 0.01 #replace value for when the uncertainty is zero
rateConv <- 365.25
delim    <- "\t"

hNum <- 1 # number of header lines in each block ** should be 1 now **
c <- file(fileN,"r") #

t_i = 1 # time index
d_i = 2 # distance index
u_i = 3 # uncertainty index
b_i = 4 # bias index
bu_i = 5 # bias uncertainty

fileLines <- readLines(c)
close(c)
nRead <- length(fileLines)
#-#-# nRead <- nlines(c)  # from parser package. Count lines in C++

# get block starts and block names
blockI <- grep("# ", fileLines)
blckNm <- sub("# ","",fileLines[blockI])
numBlck<- length(blockI)
textBlck <- vector(length=numBlck,mode="character")

for (blockNumber in 1:numBlck){
  if (blockNumber==numBlck) {enI <- nRead}
  else{enI <- blockI[blockNumber+1]-1}
  stI <- blockI[blockNumber]+1
  textBlck[blockNumber] <- paste(fileLines[stI:enI],collapse=delim)
}

calcLRR <- function(dates,dist){
  mnN <-  3
  if (length(dates)>= mnN){
    rate <- dates
    mdl  <- lm(formula=dist~rate)
    coef <- coefficients(mdl)
    CI   <- confint(mdl,"rate",level=conLevel)*rateConv 
    rate <- coef["rate"]
    
    LRR_rates <- rate*rateConv 
    LCI <- (CI[2]-CI[1])/2 # LCI
    return(c(LRR_rates,LCI))
  }
  else{return(c(NA,NA))}
}

calcWLR <- function(dates,dist,uncy){
  mnN <-  3
  if (length(dates)>= mnN){
    rate <- dates
    mdl  <- lm(formula=dist~rate, weights=(1/(uncy^2)))
    coef <- coefficients(mdl)
    CI   <- confint(mdl,"rate",level=conLevel)*rateConv 
    rate <- coef["rate"]
    WLR_rates <- rate*rateConv 
    WCI  <- (CI[2]-CI[1])/2 # WCI
    return(c(WLR_rates,WCI))
  }
  else{return(c(NA,NA))}
}
calcNSM <- function(dates,dist){
  mnN <-  2
  if (length(dates)>= mnN){
    firstDateIdx <- which.min(dates)
    lastDateIdx  <- which.max(dates)
    NSM_dist <- dist[firstDateIdx]-dist[lastDateIdx]
    EPR_rates <- NSM_dist/(as(dates[lastDateIdx]-dates[firstDateIdx],"numeric"))*rateConv
    return(c(NSM_dist,EPR_rates))
  }
  else{return(c(NA,NA))}
}

calcSCE <- function(dist){
  mnN <-  2
  if (length(dist)>= mnN){
    SCE_dist  <-  (max(dist)-min(dist))
    return(c(SCE_dist))
  }
  else{return(c(NA))}
}

LRR <-  rep(NA,numBlck)
LCI <-  rep(NA,numBlck)
WLR <-  rep(NA,numBlck)
WCI <-  rep(NA,numBlck)
SCE <-  rep(NA,numBlck)
NSM <-  rep(NA,numBlck)
EPR <-  rep(NA,numBlck)
ECI <-  rep(NA,numBlck)

getDSAS <- function(blockText){  
  splitsTxt <- unlist(strsplit(blockText,delim))
  dates <- as(as.Date(splitsTxt[seq(t_i,length(splitsTxt),num_c)],format="%Y-%m-%d"),"numeric")
  
  # distance is additive: dist + bias_distance
  dist  <- as(splitsTxt[seq(d_i,length(splitsTxt),num_c)],"numeric") + as(splitsTxt[seq(b_i,length(splitsTxt),num_c)],"numeric")
  uncy  <- sqrt(as(splitsTxt[seq(u_i,length(splitsTxt),num_c)],"numeric")^2 +as(splitsTxt[seq(bu_i,length(splitsTxt),num_c)],"numeric")^2)#
  uncy[uncy<zRepV] <- zRepV
  
  useI  <- which(!is.na(dates)) & which(!is.na(dist)) & which(!is.na(uncy))
  dates <- dates[useI]
  dist  <- dist[useI]
  uncy  <- uncy[useI]
  LRRout   <- as.numeric(calcLRR(dates,dist))
  WLRout   <- as.numeric(calcWLR(dates,dist,uncy))
  SCE   <- calcSCE(dist)
  NSMout  <- calcNSM(dates,dist)
  return(c(LRRout,WLRout,SCE,NSMout))  
}


numPar = min(ceiling(numBlck/2),numPar) # reduce cores if necessary
numInEach = ceiling(numBlck/numPar)
endI = seq(0, numBlck, numInEach)
if(endI[length(endI)] != numBlck){
  endI[length(endI) + 1] = numBlck
}



## initialize parallelization
library(doSNOW)

c1 = makeCluster(c("localhost","localhost","localhost","localhost"),type="SOCK")
registerDoSNOW(c1)

DSASstatsAll = foreach(p=1:numPar) %dopar% {
  i=1
  DSASstats = list()
  for (b in seq(endI[p]+1,endI[p+1])){
    DSASstats[[i]] <- getDSAS(textBlck[b])
    i = i+1
  }
  DSASstats
}

b = 1
for (p in 1:numPar){
  DSASstatsPar = DSASstatsAll[[p]]
  for (dsI in 1:length(DSASstatsPar)){
    DSASstats = DSASstatsPar[[dsI]]
    LRR[b] <- DSASstats[1]
    LCI[b] <- DSASstats[2]
    WLR[b] <- DSASstats[3]
    WCI[b] <- DSASstats[4]
    SCE[b] <- DSASstats[5]
    NSM[b] <- DSASstats[6]
    EPR[b] <- DSASstats[7]
    ECI[b] <- -99
    b = b + 1
  }
}

stopCluster(c1)

statsout <- data.frame("transect_ID"=blckNm,LRR,LCI,WLR,WCI,SCE,NSM,EPR,ECI)

if (localRun){
  Rprof(NULL)
  summaryRprof(filename = "DSAS_profiler.txt",chunksize=5000)
  proc.time() -ptm
}

# output is an identifier and R variable (WPS identifier). The ouput is the name of the text file
# wps.out: output, text, output title, tabular output data to append to shapefile;
output = "output.txt"
write.table(statsout,file="output.txt",col.names=TRUE, quote=FALSE, row.names=FALSE, sep="\t")
