<p align="center">
  <img src="https://github.com/1719pankaj/Utilities/blob/main/OF1_logo.png?raw=true" alt="OF1 Logo" width="200"/>
</p>

<h1 align="center">OF1 - OnlyFormula1 App 🏎️💨</h1>

<p align="center">
  Your high-octane Android pit stop for diving deep into Formula 1 data. <br> <strong>Experience live telemetry often *minutes* ahead of the TV broadcast!</strong> 🤯
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg" alt="Platform: Android">
  <img src="https://img.shields.io/badge/Data-OpenF1 + Ergast-blueviolet.svg" alt="Data Source">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License: MIT">
</p>

---

## 🔥 Lights Out & Ahead of the Pack! 🔥

Tired of waiting for the broadcast to catch up? OF1 isn't just another F1 app – it's your **unfair advantage**. We directly tap into the raw, **live telemetry & session info** from the incredible [OpenF1 API](https://openf1.org/), often delivering data **1-2 minutes *before* it hits your TV screen!** 🤯

We blend this real-time edge with rich **historical race results & season data** from the legendary [Ergast API (via Jolpica)](https://jolpi.ca/ergast/f1/). The result? A seamless, blazing-fast experience designed for the true F1 data fanatic. 🚀

## ✨ Visual Telemetry & Key Screens ✨

See the data come alive:

<table align="center" width="100%">
  <tr>
    <td align="center" width="33%">
       <img src="https://github.com/1719pankaj/Utilities/blob/main/ss_hero.jpg?raw=true" alt="Hero Screenshot" width="200"> <br> <sub><b>Live Data/Positions</b></sub>
     </td>
    <td align="center" width="33%">
      <img src="https://github.com/1719pankaj/Utilities/blob/main/ss_landing.jpg?raw=true" alt="Landing Screen" width="200"> <br> <sub><b>Main Race Overview</b></sub>
    </td>
    <td align="center" width="33%">
      <img src="https://github.com/1719pankaj/Utilities/blob/main/ss_laps.jpg?raw=true" alt="Lap Details" width="200"> <br> <sub><b>Lap Breakdown</b></sub>
    </td>
  </tr>
</table>

## 🏁 Key Features 🏁

*   **🚀 Ahead-of-TV Live Telemetry:** Get Speed, RPM, Throttle, Brake, Gear, DRS data **faster than the broadcast!** *(via OpenF1)*
*   **Live Timing & Positions:** Real-time driver standings during live sessions. *(via OpenF1)*
*   **Live Race Hub:** Current season races, auto-scroll to the action, status indicators (🔴 Live/Upcoming, ⚫ Past, ⚪ Future).
*   **Expandable Race Cards:** Tap races to see session times (Practice, Quali, Sprint, Race). Click sessions to dive in.
*   **In-Depth Lap Data:** Lap times, sector breakdowns (with status!), pit stops, speed traps. *(via OpenF1)*
*   **Telemetry Charts 📊:** Visualize driver inputs and performance with interactive charts. *(via OpenF1 & MPAndroidChart)*
*   **Race Control Messages:** Official FIA messages as they happen. *(via OpenF1)*
*   **Team Radio Snippets 🎧:** Listen to the latest driver/team communications. *(via OpenF1)*
*   **Historical Data:** Browse past seasons and full race results. *(via Ergast/Jolpica)*
*   **Driver Info:** Headshots and team details. *(via OpenF1)*
*   **⚡ Cache-First Performance:** Aggressively caches data using RoomDB for a smooth UI, updating fresh data in the background.
*   **Modern Android Tech:** Kotlin, Coroutines, Flow, Hilt, Retrofit, Navigation Component, ViewBinding (MVVM).

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

Standard Android Studio project.

1.  Clone the repository.
2.  Open in Android Studio (ensure SDKs match `build.gradle` - `compileSdk 35`, `minSdk 31`).
3.  Build and run!

## 🙌 Contributing 🙌

Contributions, issues, and feature requests are welcome! Check the [issues page](https://github.com/1719pankaj/onlyformula1/issues).

## 📜 License 📜

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details, or check below.

<details>
<summary>MIT License Text</summary>

```
MIT License

Copyright (c) [2025] [Pankaj Kumar Roy]

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

*Get the data FIRST. Built with ❤️ and a need for speed!*
