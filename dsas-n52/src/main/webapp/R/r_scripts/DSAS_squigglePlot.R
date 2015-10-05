# wps.des: id=DSAS_squigglePlot, title=Digital Shoreline Analysis System squggle plot, abstract=plots and saves rate stats;
# wps.in: input, xml, block rates text, text input from stats with base_dist baseline_ID and tab-delimited stats with headers;
# wps.in: shortName, string, short name of statistic to plot, 3 letter string representing plot acronym;
# input is unique identifier for WPS, is a variable in R (will contain all parser text)
# xml is for WPS side of things, tells WPS how input should be formatted

localRun <- FALSE
dropVal <-  1e38
# comment this out for WPS!!!
if (localRun){
  shortName <- "LRR"
  input <- "squiggleOut.tsv"
  ptm <- proc.time() # for time of process
}
ylabel  <-  expression('Rate of change (m yr'^-1 ~')')
if (shortName=="SCE"){
  ylabel  <-  'Change envelope (m)'
}
if (shortName=="NSM"){
  ylabel  <-  'Net shoreline movement (m)'  
}

figW  <- 8
figH  <- 3.5
lM    <-.95
bM    <-.95
rM    <-.15
tM    <-.15
fRes  <- 200
fontN <- 11


fileN    <- input # will have input as a string (long string read in)
delim    <- "\t"
rateVals <- read.table(fileN,header=TRUE)

BD_i = 1 # baseline distance index
ID_i = 2 # baseline ID index
RT_i = grep(shortName,names(rateVals)) # rate index
rwBD <- rateVals[,BD_i]/1000
rwID <- rateVals[,ID_i]
rwRT <- rateVals[,RT_i]
nanI  <-  which(abs(rwRT)>=dropVal)
rwRT[nanI] <- NA
# for each NA, increment baselineID (separate segment)
if (length(nanI)>0){for (blk in 1:length(nanI)){
  stI <- nanI[blk]
  rwID[stI:length(rwID)] <- rwID[stI:length(rwID)]+1 # create break
}}

if (shortName=="LRR" | shortName=="WLR"){
  rwCI <- rateVals[,RT_i+1]
} else {
  rwCI <- rep(0,nrow(rateVals))
} # confidence intervals = 0


nLines <- length(rwBD)# total length excluding header
baseL <- duplicated(rwID)
numBase <- sum(!baseL)
indx <- seq(1,nLines)
dropI <- c(indx[!baseL],nLines)

mxY <- max(rwRT+rwCI,na.rm = TRUE)
mnY <- min(rwRT-rwCI,na.rm = TRUE)

# resort values
output = "output.png"
png(output, width=figW, height=figH, units="in",res=fRes)
par(mai=c(bM,lM,rM,tM))

plot(c(0,max(rwBD)),c(mnY,mxY),type="n",xlab="Distance alongshore (km)",ylab=ylabel,
     font=fontN,font.lab=fontN,tcl=-.2,xaxs="i",cex.lab=1.2,cex=1.3)
lines(c(0,max(rwBD)),c(0,0),col="grey24",lwd=1.2,pch=1,lty=2)

for (p in 1:numBase){
  indx_1 <- dropI[p]
  indx_2 <- dropI[p+1]-1
  dist <- rwBD[indx_1:indx_2]
  rate <- rwRT[indx_1:indx_2]
  CI_up <- rate+rwCI[indx_1:indx_2]
  CI_dn <- rate-rwCI[indx_1:indx_2]
  usI <- which(!is.na(rate))
  if (shortName=="LRR" | shortName=="WLR"){
    
    polygon(c(dist[usI],
              rev(dist[usI])),
              c(CI_up[usI],
                rev(CI_dn[usI])),
              col="grey",border=NA)
  }
  
  lines(dist,rate,lwd=2.5)
}

if (localRun) {proc.time() - ptm}
dev.off()
# output is an identifier and R variable (WPS identifier). The ouput is the name of the text file
# wps.out: output, png, Squiggle Plot, png plot of shoreline rates;