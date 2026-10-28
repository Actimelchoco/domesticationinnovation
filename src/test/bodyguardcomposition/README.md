
Bodyguard profiles: new roster members default to mode `bodyguard` and movement
`close`. Owners can change them with `/tames mode <pet> <mode>` and
`/tames movement <pet> <default|skeleton|close>`, including bulk selectors.
Choices persist through maintenance/save-load. Owner health below 5 temporarily
forces close movement, then restores the chosen profile. Sit/wander/guardian
orders remain locked for assigned roster bodyguards.

Run `.\gradlew.bat --no-daemon -I gradle/bodyguardProfilesRegression.init.gradle runGameTestServer --offline`
for the 14 composition and bodyguard tests, including profile/default/legacy NBT,
group command, emergency recovery, and original-profile restoration checks.
