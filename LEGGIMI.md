# Conta Giorni per Android

App nativa con widget per la schermata Home: giorni mancanti o passati, foto di sfondo,
frase personale e musica con pulsante play/pausa direttamente sul widget.

## Come ottenere l'app sul telefono

1. Installa Android Studio (gratuito): https://developer.android.com/studio
2. Apri la cartella `ContaGiorniAndroid` con File > Open e aspetta la sincronizzazione di Gradle
   (scarica da solo quello che serve; se chiede il JDK scegli la versione 17 inclusa).
3. Collega il telefono con il debug USB attivo e premi Run, oppure crea un file APK da
   Build > Build Bundle(s) / APK(s) > Build APK(s) e installalo sul telefono.
4. Sul telefono: tieni premuta la Home > Widget > Conta Giorni, oppure apri l'app e premi
   "Aggiungi un widget alla Home".

## Come si usa

- Appena aggiungi il widget si apre la schermata di personalizzazione:
  titolo, data, frase, stile (Foto, Colore, Chiaro), colore, immagine, musica.
- Tocca il widget per modificarlo. Tocca il pulsante in alto a destra per play/pausa.
- Un solo brano suona alla volta. Mentre suona c'e' una notifica con il pulsante "Ferma".
- Il widget si aggiorna da solo a mezzanotte e ogni 30 minuti circa.

## Note

- Foto e brani vengono copiati nella memoria privata dell'app (brano: massimo 40 MB).
- Il progetto non e' stato compilato ne' provato su un telefono. Se Android Studio segnala
  errori, copiali e mandameli.
