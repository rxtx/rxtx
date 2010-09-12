Setting up a Microsoft Visual C Project for a Windows DLL
---------------------------------------------------------

1. Create a new project in the path\to\rxtx\env\msvc folder.
   Project type is Win32 Dynamic Link Library. Choose an empty DLL if asked.
   Delete any source files that are created - they won't be needed.

2. In Project->Settings->C/C++->Preprocessor add additional include directories:

    path\to\rxtx\src\c\include, path\to\jdk\include, path\to\jdk\include\win32

3. In Project->Add To Project->Files... add the RXTX and Windows source files to the project:

    path\to\rxtx\src\c\include\*.*
    path\to\rxtx\src\c\windows\*.*

4. In Project->Settings->Link->General set the output file name to RXTXnative.dll.

5. Build the project.
