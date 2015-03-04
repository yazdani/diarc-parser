#!/bin/bash

#nohup ./runscripts/runspeechrectraining.sh &> speechrectraining.out &

for i in TEST;
do
    echo "RUN $i"
    #LD_LIBRARY_PATH=com/nsim/speechrec ./runadeserver com.nsim.speechrec.nsimSpeechrecServer -buildliq 7 20 7 -train richard_buxcorpus/richard.lst 9 -test richard_buxcorpus/richard.lst -savero richard_buxcorpus/richard_barmic$i.ro -saveliq richard_buxcorpus/richard_barmic$i.liq -offline
#    LD_LIBRARY_PATH=com/nsim/speechrec ./runadeserver com.nsim.speechrec.nsimSpeechrecServer -buildliq 6 20 6 -train tom_singlewords_cut/tom.lst 10 -test tom_singlewords_cut/tom.lst -savero tom_singlewords_cut/tom_singlewords$i.ro -saveliq tom_singlewords_cut/tom_singlewords$i.liq -offline
    #LD_LIBRARY_PATH=com/nsim/speechrec ./runadeserver com.nsim.speechrec.nsimSpeechrecServer -buildliq 3 10 3 -train tom_wireless/tom.lst 1 -savero tom_wireless/tom_wireless$i.ro -saveliq tom_wireless/tom_wireless$i.liq -offline
    LD_LIBRARY_PATH=com/nsim/speechrec ./runadeserver com.nsim.speechrec.nsimSpeechrecServer -buildliq 3 10 3 -train tom_wireless_fullphrases/tom.lst 1 -savero tom_wireless_fullphrases/tom_wireless_fullphrases$i.ro -saveliq tom_wireless_fullphrases/tom_wireless_fullphrases$i.liq -offline
done