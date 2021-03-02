Applications for common or useful tasks, such as resizing images, removing lens distortion in batch, ...etc.

Building:

```bash
cd boofcv
./gradlew applicationsJar
java -jar applications/applications.jar
```

Then it will print something like:
```bash
Trying to run a command-line application?  Here are your options!

  CreateFiducialSquareImage
  CreateFiducialSquareBinary
  CreateFiducialRandomDot
...
```
Next you will need to select which tool / application from the list that you want to run. Then more help will be
printed. If instructions are not clear feel free to post BoofCV message board to get help or provide advice
on how to make the instructions easier to understand.
