# Family Tree
### _Your family story, beautifully kept._

> Rebrand + Premium (Monthly / Lifetime) plan and module map: see [docs/REBRAND_PLAN.md](docs/REBRAND_PLAN.md).
> Modules: `app`, `core/designsystem` (brand tokens, theme, components), `core/billing` (Google Play Billing), `cropper`.

Family Tree is an app for Android designed to manage family trees.
At the moment it's completely free and open source.

## Features
With Family Tree you can:
- Create a family tree from scratch, entering names, dates, places, various events, photos and sources.
- Import an existing family tree through a GEDCOM file and modify it as you want.
- Export the family tree you created (via GEDCOM again) to import in every other genealogy program.
- Share a tree with your relatives, letting them improve it and receiving back the updates. Then you can choose whether accept them or not.

Minimum supported version is Android 7.0 Nougat (API 24), target is Android 17 (API 37).
The intent is that data structure respects as much as possible the latest version of GEDCOM standard: [5.5.1](https://www.familysearch.org/developers/docs/gedcom/) and possibly also [5.5.5](https://www.gedcom.org/gedcom.html).<br>
Family Tree is strongly based on the library [Gedcom 5 Java](https://github.com/FamilySearch/gedcom5-java) by FamilySearch.

## Building
1. Open the project in Android Studio, or run `./gradlew assembleDebug`.
2. `app/google-services.json` is a **dummy** so the project builds. Firebase (Analytics, Crashlytics, Messaging, Performance) is not
   connected until you replace it with the file of your own Firebase project for package `dna.familytree`.
3. Release builds need no setup: without `keystore.properties` they are signed with the debug key and use Google's sample AdMob IDs.
   To sign with your own key and use your own ad units, copy `keystore.properties.example` to `keystore.properties` (git-ignored) and fill it in.

## License
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

## About
Family Tree is written in Java within Eclipse and Android Studio.  
Project started on beginning of 2018.  
Author is Michele Salvador, an Italian programmer and genealogy enthusiast.
