$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
$env:ANDROID_HOME = "E:\Android\android-sdk"
$env:ANDROID_SDK_ROOT = "E:\Android\android-sdk"

"sdk.dir=E\:/Android/android-sdk" | Set-Content "local.properties"

Write-Output "Running local unit tests with Gradle..."
& .\gradlew.bat testDebugUnitTest
