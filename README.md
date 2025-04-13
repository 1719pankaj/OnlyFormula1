<p align="center">
  <img src="https://github.com/1719pankaj/Utilities/blob/main/OF1_logo.png?raw=true" alt="OF1 Logo" width="200"/>
</p>

<h1 align="center">OF1 - OnlyFormula1 App 🏎️💨</h1>

<p align="center">
  Your high-octane Android pit stop for diving deep into live & historical Formula 1 data!
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg" alt="Platform: Android">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License: MIT">
</p>

---

## 🔥 Get Ready for Lights Out! 🔥

OF1 isn't just another F1 app. It's built for the true fans who crave *all* the data. We mash up **live telemetry & session info** from the awesome [OpenF1 API](https://openf1.org/) with rich **historical race results & season data** from the legendary [Ergast API (via Jolpica)](https://jolpi.ca/ergast/f1/). The result? A seamless, blazing-fast experience to level up your F1 obsession. 🚀

## ✨ Visual Telemetry ✨

Get a glimpse of the action:

| Landing / Main Screen                 | Lap Details Screen                       |
| :------------------------------------: | :--------------------------------------: |
| ![Landing Screen](https://github.com/1719pankaj/Utilities/blob/main/ss_landing.jpg?raw=true) | ![Lap Details](https://github.com/1719pankaj/Utilities/blob/main/ss_laps.jpg?raw=true) |
| **Main Race Overview**                | **Detailed Lap Breakdown**                |

*(Hero Screenshot showcasing various data points)*
![Hero Screenshot](https://github.com/1719pankaj/Utilities/blob/main/ss_hero.jpg?raw=true)

## 🏁 Key Features 🏁

*   **Live Race Hub:** See the current season's races, automatically scroll to the live/next event, and get status indicators (🔴 Live/Upcoming, ⚫ Past, ⚪ Future).
*   **Expandable Race Cards:** Tap a race to instantly see session times (Practice, Quali, Sprint, Race).
*   **Session Hopping:** Tap a specific session to jump straight into the live/historical position data.
*   **Live Timing & Positions:** Real-time driver standings during live sessions. *(via OpenF1)*
*   **Detailed Lap Data:** Dive into individual lap times, sector breakdowns (with status!), pit stops, and crucial speed trap info. *(via OpenF1)*
*   **Telemetry Charts 📊:** Visualize driver performance with interactive charts for Speed, RPM, Throttle, Brake, Gear, and DRS usage. *(via OpenF1 & MPAndroidChart)*
*   **Race Control Messages:** Stay updated with official messages during live sessions. *(via OpenF1)*
*   **Team Radio Snippets 🎧:** Listen to the latest radio messages for each driver. *(via OpenF1)*
*   **Historical Data:** Browse past seasons and race results. *(via Ergast/Jolpica)*
*   **Driver Info:** Headshots and team details for the current session. *(via OpenF1)*
*   **⚡ Cache-First Performance:** Data is aggressively cached using RoomDB, making the UI snappy even when APIs are slow or offline. Fresh data is fetched seamlessly in the background.
*   **Modern Android Tech:** Built with Kotlin, Coroutines, Flow, Hilt, Retrofit, Navigation Component, and ViewBinding following MVVM principles.

## 🔧 Tech Stack & Architecture 🔧

*   **Language:** Kotlin
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Async:** Kotlin Coroutines & Flow (StateFlow, SharedFlow)
*   **Dependency Injection:** Hilt
*   **Networking:** Retrofit, OkHttp, Gson
*   **Database:** Room Persistence Library
*   **UI:** Android Views (XML), ViewBinding, Navigation Component, RecyclerView, Material Components
*   **Charting:** MPAndroidChart
*   **State Management:** Custom `Resource` class (Loading, Success, Error)

## 💾 Data Sources 💾

*   **Live/Session Data:** [OpenF1 API](https://api.openf1.org/v1/) (Positions, Laps, Intervals, Telemetry, Radio, etc.)
*   **Historical Data:** [Ergast API](http://ergast.com/mrd/) (via [Jolpica Proxy](https://api.jolpi.ca/ergast/f1/)) (Seasons, Race Schedules, Results)

## 🛠️ Setup 🛠️

This is a standard Android Studio project.

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Build and run! (Requires Android SDK corresponding to `compileSdk = 35` and `minSdk = 31`).

## 🙌 Contributing 🙌

Contributions, issues, and feature requests are welcome! Feel free to check [issues page](https://github.com/YOUR_USERNAME/YOUR_REPONAME/issues).

## 📜 License 📜

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details, or check below.

<details>
<summary>MIT License Text</summary>

```
MIT License

Copyright (c) [yyyy] [fullname]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

</details>

---

*Built with ❤️ and a need for speed!*
