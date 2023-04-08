-keep class dna.familytree.Settings, dna.familytree.list.FamiliesFragment, dna.familytree.list.SubmittersFragment # for R8.fullMode
-keepclassmembernames class dna.familytree.Settings, dna.familytree.Settings$Tree, dna.familytree.Settings$Diagram, dna.familytree.Settings$ZippedTree, dna.familytree.Settings$Share { *; }
-keepclassmembers class org.folg.gedcom.model.* { *; }
#-keeppackagenames org.folg.gedcom.model # Gedcom parser lo chiama come stringa eppure funziona anche senza
-keepattributes LineNumberTable,SourceFile # per avere i numeri di linea corretti in Android vitals

#-printusage build/usage.txt # risorse che vengono rimosse
#-printseeds build/seeds.txt # entrypoints
