#!/bin/bash

#LD_LIBRARY_PATH=com/nsim/speechrec ./runadeserver com.nsim.speechrec.nsimSpeechrecServer -loadro richard_buxcorpus/richard_barmic2.ro -loadliq richard_buxcorpus/richard_barmic2.liq -micnum 1

#LD_LIBRARY_PATH=com/nsim/speechrec ./runadeserver com.nsim.speechrec.nsimSpeechrecServer -loadro richard_buxcorpus/richard_barmic2.ro -loadliq richard_buxcorpus/richard_barmic2.liq -offline

#LD_LIBRARY_PATH=com/nsim/speechrec ./runadeserver com.nsim.speechrec.nsimSpeechrecServer -loadro richard_barmic2.ro -loadliq richard_barmic2.liq -testfile richard_buxcorpus_TEST.wav -offline

#./runadeserver com.simspeech.SimSpeechInputServer -g -cfg simspeechtestfiles &

#LD_LIBRARY_PATH=com/nsim/speechrec ./runadeserver com.nsim.speechrec.nsimSpeechrecServer -loadro tom_wireless/tom_wireless11.ro -loadliq tom_wireless/tom_wireless11.liq -g -offline


#LD_LIBRARY_PATH=com/nsim/speechrec ./runadeserver com.nsim.speechrec.nsimSpeechrecServer -loadro tom_wireless/tom_wireless11.ro -loadliq tom_wireless/tom_wireless11.liq -g -offline

LD_LIBRARY_PATH=com/nsim/speechrec ./runadeserver com.nsim.speechrec.nsimSpeechrecServer -loadro tom_wireless/tom_barmic1.ro -loadliq tom_wireless/tom_wirle1.liq -g -micnum 1

#LD_LIBRARY_PATH=com/nsim/speechrec ./runadeserver com.nsim.speechrec.nsimSpeechrecServer -loadro tom_singlewords_cut/tom_singlewords0.ro -test tom_singlewords_cut/tom.lst -loadliq tom_singlewords_cut/tom_singlewords0.liq -offline