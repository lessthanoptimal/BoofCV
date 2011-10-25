#/bin/sh
for A in *.png; do
base=`basename $A .png`
echo "$base.pgm"
convert "$A" "$base.pgm"
done