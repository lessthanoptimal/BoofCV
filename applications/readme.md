Applications for common or useful tasks, such as resizing images, removing lens distortion in batch, ...etc.

Usage:

```bash
cd boofcv
./gradlew applicationsJar
java -jar applications/applications.jar
```

Then follow printed instructions.

Several applications output .ps files, which are postscript files. In Linux they are very easy to read. MacOS it should be too. In Windows they are a pain to deal with.

Windows Instructions:
1) Download and Install Gimp
2) Open the .ps file
3) Set resolution to 300 or 600 DPI
4) View and print
