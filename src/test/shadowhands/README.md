# Shadow Hands regression

Run `.\gradlew.bat --no-daemon -I gradle/shadowHandsRegression.init.gradle runGameTestServer --offline`.

Checks actual target damage, three initialized hands, and cleanup after removing the ability for both an interface tame (fox) and a vanilla tame (wolf). The fox test fails with the original TamableAnimal-only effect lookup.

After running, use `.\gradlew.bat cleanCompileJava cleanProcessResources build --offline` for a release without the test fixtures.
