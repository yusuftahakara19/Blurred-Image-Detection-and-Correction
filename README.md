## Blurred-Image-Detection-and-Correction

1. **Download and Install Android Studio:** Download the latest version of Android Studio from the [Android Developer website](https://developer.android.com/studio) and follow the installation instructions.

2. **Open Android Studio and Create a New Project:** Open Android Studio and select "Create New Project" to start a new project. Configure the basic settings for your project.

3. **Adding a Java Class to the "app" Module:** Right-click on the "app" module in your project, then choose "New" and select "Java Class." Provide a name for the class and click "OK."

4. **Replace the Java Class Content:** Replace the content of the newly created Java class with the code from the files.

5. **Create XML Layout File:** Under the "res/layout" directory, right-click and create a new XML file with the same name you used for the Java Class you created earlier. Replace the content of this XML file with the code from the relevant XML files.

6. **Adding OpenCV Library:** To make the project work, you need to include the OpenCV library. Open your project's "build.gradle" file and add the following line inside the "dependencies" block:

```gradle
dependencies {
    // Other dependencies...
    implementation 'org.opencv:opencv:4.5.3'
}
This will add the OpenCV library to your project.

With these steps completed, you have successfully compiled and set up the project to run with the provided source code.

[Application Introduction Video](https://www.youtube.com/shorts/HEuNIHoW5E8) 
