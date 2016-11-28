## DATA FILES


Before you attempt to run any example make sure you have all the data files!  See boofcv/README.md for how to checkout the data directory.

## Gradle Instructions: Linux/Unix

To build all the examples and run the easy to use example application launcher do the following

```bash
cd boofcv/examples
gradle examples
java -jar examples.jar
```

Alternatively, to run a specific example outside the application launcher:
```bash
java -cp examples.jar boofcv.examples.imageprocessing.ExampleBinaryOps
```

