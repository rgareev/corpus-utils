#!/bin/bash
java -cp lib/${project.build.finalName}.jar ru.kfu.itis.issst.corpus.utils.DocToFile "$@"