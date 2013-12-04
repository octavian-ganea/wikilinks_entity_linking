#!/bin/bash

ant -f create_jar_script.xml
scp ./extract.jar ganeao@cisco1.ethz.ch:crosswiki/lib
