-keep class dna.familytree.Settings, dna.familytree.list.FamiliesFragment, dna.familytree.list.SubmittersFragment # for R8.fullMode
-keepclassmembernames class dna.familytree.Settings, dna.familytree.Settings$Tree, dna.familytree.Settings$Diagram, dna.familytree.Settings$ZippedTree, dna.familytree.Settings$Share, dna.familytree.Settings$TreeSettings, dna.familytree.Settings$Reminder, dna.familytree.Settings$Occasion, dna.familytree.Settings$Birthday { *; }
# Read by Gson: fields and the no-arg constructor (which applies the defaults) must stay.
# Settings$Birthday was obfuscated before 1.6: entries saved then can't be read and are rebuilt (ReminderScheduler.repairBirthdays).
-keepclassmembers class dna.familytree.Settings$TreeSettings, dna.familytree.Settings$Reminder, dna.familytree.Settings$Occasion, dna.familytree.Settings$Birthday {
    <init>();
    <fields>;
}
-keepclassmembers class org.folg.gedcom.model.* { *; }
#-keeppackagenames org.folg.gedcom.model # Gedcom parser lo chiama come stringa eppure funziona anche senza
-keepattributes LineNumberTable,SourceFile # per avere i numeri di linea corretti in Android vitals

#-printusage build/usage.txt # risorse che vengono rimosse
#-printseeds build/seeds.txt # entrypoints

# Optional dependencies of joda-time and slf4j, not used by the app (R8 errors on missing classes from AGP 9)
-dontwarn org.joda.convert.FromString
-dontwarn org.joda.convert.ToString
-dontwarn org.slf4j.impl.StaticLoggerBinder
