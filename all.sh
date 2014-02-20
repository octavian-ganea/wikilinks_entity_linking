#!/bin/bash

ant -f create_jar_script.xml
scp ./extract.jar ganeao@cisco1.ethz.ch:wikilinks_project/lib
#scp ./extract.jar ganeao@cisco1.ethz.ch:/mnt/local/ganeao/lib
#scp ./extract.jar ganeao@cisco2.ethz.ch:/mnt/local/ganeao/lib
