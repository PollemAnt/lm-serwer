# lm-serwer

> 🇵🇱 Polski opis poniżej  
> 🇬🇧 English version below

---

# List Miłosny – Serwer Multiplayer (PL)

Backend serwera do gry multiplayer na Androida **„List Miłosny”**.  
Serwer odpowiada za logikę gry, synchronizację stanu pomiędzy graczami
oraz komunikację w czasie rzeczywistym.

Projekt jest częścią większego systemu:

- 📱 aplikacja kliencka (Android)
- 🌐 serwer multiplayer (ten projekt)

---

## 🎮 O grze

**List Miłosny** to cyfrowa wersja karcianej gry multiplayer przeniesiona
na platformę Android.

Gracze biorą udział we wspólnej rozgrywce, w której zagrywają karty.
Każda karta posiada **unikalny efekt**, który wpływa na przebieg gry
oraz innych graczy.

Aby zapewnić uczciwość i spójność rozgrywki, cała logika gry
realizowana jest po stronie serwera.

---

## 🧠 Rola serwera

Serwer jest odpowiedzialny za:

- tworzenie i zarządzanie pokojami gry
- obsługę graczy 
- przechowywanie aktualnego stanu gry
- walidację ruchów graczy
- rozsyłanie aktualizacji stanu do wszystkich klientów
- utrzymanie jednej „prawdy” o stanie gry (single source of truth)

Klient Android **nie zawiera logiki gry** – całość odbywa się po stronie serwera.

---

## 🛠 Stack technologiczny

- **Kotlin**
- **Ktor** – framework serwerowy
- **Gradle**
- **Docker**
- **REST** – zmiana stanu gry
- **WebSockets** – obsługa zdarzeń i synchronizacja w czasie rzeczywistym

---

## 📡 Komunikacja z klientem

Serwer komunikuje się z aplikacją Android poprzez:

- HTTP (REST) – operacje takie jak tworzenie pokoju gry czy dołączanie graczy
- WebSockets – synchronizacja stanu gry w czasie rzeczywistym

---

## 🚀 Budowanie i uruchamianie

Projekt został wygenerowany przy użyciu
[Ktor Project Generator](https://start.ktor.io).

| Task                          | Description                                                          |
| ----------------------------- | -------------------------------------------------------------------- |
| `./gradlew test`              | Uruchomienie testów                                                  |
| `./gradlew build`             | Zbudowanie projektu                                                  |
| `buildFatJar`                 | Zbudowanie wykonywalnego JAR-a z zależnościami                       |
| `buildImage`                  | Zbudowanie obrazu Docker                                             |
| `publishImageToLocalRegistry` | Publikacja obrazu Docker lokalnie                                    |
| `run`                         | Uruchomienie serwera                                                 |
| `runDocker`                   | Uruchomienie serwera w Dockerze                                      |

---

# List Miłosny – Multiplayer Server (EN)

Backend server for the Android multiplayer game **“List Miłosny”**.  
The server is responsible for game logic, game state synchronization
between players, and real-time communication.

This project is part of a larger system:

- 📱 Android client application
- 🌐 multiplayer server (this repository)

---

## 🎮 About the game

**List Miłosny** is a digital adaptation of a multiplayer card game
designed for Android devices.

Players participate in a shared game session and play cards during
their turns. Each card has a **unique effect** that influences the game
flow and other players.

To ensure fairness and consistency, all core game logic
is handled exclusively by the server.

---

## 🧠 Server responsibilities

The server is responsible for:

- creating and managing game rooms
- handling players (joining / leaving)
- storing the current game state
- validating player actions
- broadcasting game state updates to all clients
- maintaining a single source of truth for the game state

The Android client **does not contain game logic** – everything is handled server-side.

---

## 🛠 Tech stack

- **Kotlin**
- **Ktor** – server framework
- **Gradle**
- **Docker**
- **REST** – game state changes
- **WebSockets** – events and real-time synchronization

---

## 📡 Client communication

The server communicates with the Android client via:

- HTTP (REST) – operations such as creating and joining game rooms
- WebSockets – real-time game state synchronization

---

## 🚀 Building & running

This project was created using the
[Ktor Project Generator](https://start.ktor.io).

| Task                          | Description                                                          |
| ----------------------------- | -------------------------------------------------------------------- |
| `./gradlew test`              | Run tests                                                            |
| `./gradlew build`             | Build the project                                                    |
| `buildFatJar`                 | Build an executable JAR with all dependencies                        |
| `buildImage`                  | Build the Docker image                                               |
| `publishImageToLocalRegistry` | Publish the Docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run the server using Docker                                          |
