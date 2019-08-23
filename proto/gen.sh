#!/bin/sh

rm -rf io

protoc --java_out=./ PBContent.proto

mv io ../src/test/java


