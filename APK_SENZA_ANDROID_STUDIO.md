# Ottenere l'APK senza installare Android Studio

Serve un account GitHub gratuito. GitHub compila l'app per te e ti da' il file APK.

1. Su github.com crea un nuovo repository (anche privato).
2. Decomprimi lo zip e carica tutto il contenuto nel repository (Add file > Upload files).
   Controlla che sia stata caricata anche la cartella `.github` (dentro c'e' `workflows/build-apk.yml`).
   Se manca, crea il file a mano con Add file > Create new file, nome
   `.github/workflows/build-apk.yml`, e incolla il contenuto del file omonimo.
3. Apri la scheda Actions. Il lavoro "Build APK" parte da solo (oppure premi Run workflow).
   Ci vogliono circa 5-10 minuti.
4. Quando e' verde, aprilo e scarica l'allegato "ContaGiorni-APK" in fondo alla pagina.
   Dentro lo zip c'e' `app-debug.apk`.
5. Copia l'APK sul telefono e aprilo. Android chiedera' di consentire l'installazione da
   questa fonte: accetta.
6. Tieni premuta la Home > Widget > Conta Giorni.

Il file `logo.png` (512 x 512) e' il logo dell'app.
