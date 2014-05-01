#!/bin/bash

ant -f create_jar_script.xml
scp -r run_config/ ganeao@cisco2.ethz.ch:/mnt/SG/ganeao/wikilinks_project/
scp ./extract.jar ganeao@cisco2.ethz.ch:/mnt/SG/ganeao/wikilinks_project/lib/
#scp ./extract.jar ganeao@cisco2.ethz.ch:contexts/lib
#scp ./extract.jar ganeao@cisco1.ethz.ch:/mnt/local/ganeao/lib
#scp ./extract.jar ganeao@cisco2.ethz.ch:/mnt/local/ganeao/lib
