java -Xmx2048M -cp com/discourse/jar/weka.jar:com/discourse/mink:com/discourse/jar:. com/discourse/mink/TrainingParser -config $1 -trainingfile $2 -outfile test.instances
java -cp com/discourse/jar/weka.jar:. weka.core.converters.CSVLoader test.instances.action > $2.action.arff
java -cp com/discourse/jar/weka.jar:. weka.core.converters.CSVLoader test.instances.label > $2.label.arff
#rm test.instances