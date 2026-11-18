# Launcher artwork

How Pulse's launcher icon and banner are made, and why the package name must not change casually.

Fire TV draws sideloaded apps' icons in a square centered on a gray 16:9 tile and squeezes
non-square images, so `ic_launcher.png` (`app/src/main/res/drawable-nodpi/`) is a transparent
square with the "Pulse" lockup. The 16:9 `banner.png` (`app/src/main/res/drawable-xhdpi/`) is still
declared in `app/src/main/AndroidManifest.xml` for Android TV / Google TV launchers. Both are
rendered by `tools/RenderBranding.java` (usage in its header).

The Fire TV launcher caches an app's icon per package name and does not refresh it on update,
reinstall or reboot; that is why the package is `com.marcoelizalde.pulse.tv` (`applicationId` in
`app/build.gradle.kts`). Get the artwork right before installing under a new package name.
