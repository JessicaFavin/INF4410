#!/bin/bash

debut="$(date +%s)"
for i in `seq 1 40`;
do
        wget -q 172.15.105.191:8000&
done
wait
fin="$(date +%s)"
let "temps = fin - debut"
echo $temps
