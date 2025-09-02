# Fso Android Wrapper
=======================<br />
Android wrapper for running Freespace Open on Android<br />
<br />
Note: This is experimental, do not expect a fully playable game out of this.<br />
Follow the progress here: https://github.com/scp-fs2open/fs2open.github.com/pull/6992

<br /><br />
How to use it
=======================

<br />
1) Build FSO for Android (From Linux X64)<br /><br />
-Clone this repo/branch: https://github.com/Shivansps/fs2open.github.com/tree/android-build<br />
-Follow the instructions here to get the dependencies https://github.com/scp-fs2open/fs2open.github.com/wiki/Building-on-Linux<br />
-Build the embedfile, by building the FSO tools first. Follow the instrctions on the previous step but instead of the normal cmake command, use "cmake -DFSO_BUILD_TOOLS .."<br />
-Get the prebuilt libs fso needs for android: https://drive.google.com/file/d/1Yq2veycfhbUKd3MGnRF3-S46Y2ntzHR_/view?usp=sharing<br />
-Get the android NDK: https://developer.android.com/ndk/downloads<br />
-Compile FSO with: <br />
cmake .. -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=/path/to/ndk/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-31 -DEMBEDFILE_PATH=/path/to/embedfile -DFSO_PREBUILT_OVERRIDE=/path/to/prebuilt/libs/folder -G Ninja && sed -i 's/-lusb-1.0//' build.ninja
<br /><br />

2) Copy: "libavcodec.so, libavformat.so, libavutil.so, libfs2_open_24_3_0_arm64.so, libopenal.so, libSDL2.so, libswresample.so, libswscale.so" from the bin folder to this project "Fso_Android_Wrapper\app\src\main\jniLibs\arm64-v8a" folder.
<br />

3) From here you can build/deploy/whatever

<br /><br />

4) Default folder for game assets FSO can see and is accessible via USB folder browsing is "[phone][internal storage]\Android\data\com.shivansps.fsowrapper\files". The path is the same for sdcard or external usb drives.

<br /><br />

5) Keep in mind OpenGL ES will never be able to run the built-in FSO shaders, you need to include/use external shaders changed for OpenGL ES. The wrapper already include 0_shaders_v1.vp in its assets folder and it will always copy it to any workfolder before launching the app.
