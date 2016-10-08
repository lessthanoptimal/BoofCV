# Auto Generating Code

Simply install protobufs on your system and invoke the following command to generate java code

```
protoc -I=./ --java_out=../src ./calibration.proto
```
