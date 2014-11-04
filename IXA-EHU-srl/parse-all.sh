DIR=$1

echo "The program will parse `find $DIR -type f | wc -l` files."
echo "The result of file X will be file X.out in the same directory."
echo "Started at: `date`"
find $DIR -type f -exec sh -c "echo -n '{}... ' && cat '{}' | java -cp IXA-EHU-srl-1.0.jar ixa.srl.SRLClient > '{}.out' && echo 'Done.'" \;
echo "Finished at: `date`"
