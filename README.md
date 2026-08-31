| 🧑‍🎓 نام و نام خانوادگی | 💳 شماره دانشجویی |
| :---: | :---: |
| **امیرحسین بازدار** | `404105575` |
| **آرش یوسف‌نژاد** | `404110248` |
| **ایلیا اصلاحی** | `404172292` |

## Running the game

Accounts live on the server from Phase 3 onwards, so **the server has to be running before you
log in or register** — the client cannot create an account without it.

Start the server first, in its own terminal, and leave it running:

```bash
./gradlew runServer
```

Then start the graphical client in a second terminal:

```bash
./gradlew runGdx
```

For the terminal version instead of the graphical one:

```bash
./gradlew runTerminal
```

### Debug mode

Adds the on-screen cheat buttons (coins, diamonds, sun, plant food) and an **Unlock Chapters**
button that opens the whole adventure map, so every chapter, special stage and boss can be reached
without playing through the earlier levels:

```bash
./gradlew runGdx -Ppvz.debug=true
```

The same unlock is available in the terminal version from the Game menu:

```
menu cheat unlock-chapters
```

The server listens on port 7070 by default. To use another port, start it with
`./gradlew runServer -Pport=8080`.

## Building and testing

```bash
./gradlew build
```
