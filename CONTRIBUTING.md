<p align="center">
  <img src="https://github.com/1719pankaj/Utilities/blob/main/OF1_logo.png?raw=true" alt="OF1 Logo" width="200"/>
</p>

<h1 align="center">OF1 - OnlyFormula1 App 🏎️💨</h1>

<p align="center">
  Your high-octane Android pit stop for diving deep into Formula 1 data. <br> Experience live telemetry often <strong>minutes ahead of the TV broadcast!</strong> 🤯
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg" alt="Platform: Android">
  <img src="https://img.shields.io/badge/Kotlin-100%25-blueviolet.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/Architecture-MVVM + Clean-orange.svg" alt="Architecture">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License: MIT">
</p>

---

## 🔥 Welcome to the Pit Crew! 🔥

Thanks for checking out the OF1 project! We're building a data-driven F1 experience, leveraging live telemetry and historical results. This app dives deep into modern Android practices like Kotlin Flows, Hilt, and Room, fused with data from multiple APIs.

**The core mission:** Deliver F1 data **fast**, often beating the TV broadcast, with a slick, reactive UI.

If you're comfortable with the modern Android toolkit and want to contribute to a project pushing the boundaries of F1 data access, you're in the right place!

## 🚦 Contribution Lanes

*   **🐞 Bug Reports:** Precise reports help us stay on track. (See "Reporting Bugs")
*   **💡 Feature Pitches:** Suggest new features or improvements via Issues.
*   **💻 Code Contributions:** Tackle bugs or features via Pull Requests. (See "Code Contributions")
*   **📄 Doc Enhancements:** Help clarify the README, code comments, or this guide.

## 🛠️ Lap 1: Setup

We assume you have a standard Android development environment (Android Studio, Git, relevant SDKs).

1.  **Fork & Clone:** Fork the repo and clone your fork.
    ```bash
    git clone https://github.com/YOUR_USERNAME/onlyformula1.git
    cd onlyformula1
    ```
2.  **Open & Sync:** Open the project in Android Studio. Let Gradle do its thing.
3.  **SDKs:** The project targets `compileSdk = 35` and `minSdk = 31`. Ensure you have SDK 35 installed.
4.  **Signing:** Uses the standard debug keystore (`~/.android/debug.keystore`) by default, typically handled automatically by AS for debug builds.
5.  **Build & Run:** Select the `app` configuration and run on an emulator (API 31+) or device.

## 🐛 Reporting Bugs

Precision is key for a quick fix!

1.  **Search Issues:** Avoid duplicates by checking existing [GitHub Issues](https://github.com/1719pankaj/onlyformula1/issues).
2.  **New Issue:**
    *   **Title:** Clear, concise summary.
    *   **Steps to Reproduce:** Essential! Be exact.
    *   **Expected vs. Actual:** Describe the discrepancy.
    *   **Logs:** **Crucial!** Provide relevant Logcat output, especially stack traces for crashes. Filter by the app's package name (`com.example.of1`) if possible.
    *   **Environment:** App Version (`versionName` in `app/build.gradle.kts`), Device, Android OS version.
    *   **Screenshots/Videos:** Highly recommended.

## ✨ Suggesting Enhancements

Got a DRS-worthy feature idea?

1.  **Search Issues:** Check if it's already proposed.
2.  **New Issue:**
    *   **Title:** Clear feature name.
    *   **Description:** Detail the feature, its benefits, and potential implementation ideas. Mockups welcome! Use the `enhancement` label.

## 💻 Code Contributions: Navigating the Track

This app has several moving parts. Understanding these core concepts will help you navigate:

**Key Architectural Points:**

1.  **MVVM & Clean(ish):**
    *   **UI (Fragments):** Observe ViewModel state (`StateFlow`), handle user input, navigation. Minimal logic.
    *   **ViewModel:** Expose UI state (`StateFlow`), handle UI events, fetch data from Repositories, contain presentation logic.
    *   **Repository:** Single source of truth for data. Implements the Cache-First strategy, interacts with DAOs and API Services.
    *   **Data Sources:** Remote (`ApiService`) and Local (`DAO`).
2.  **Dual APIs & Bridging:**
    *   **Jolpica/Ergast (`JolpicaApiService`):** Used for historical data (past seasons, race lists, results). See `RaceRepository`, `ResultRepository`.
    *   **OpenF1 (`OpenF1ApiService`):** Used for live/recent *session-specific* data (positions, laps, telemetry, radio, drivers-in-session, etc.). See `PositionRepository`, `LapRepository`, `SessionRepository`, etc.
    *   **The Bridge:** Logic often exists in ViewModels (like `MainViewModel.findSessionAndPrepareNavigation`) or Fragments (`RacesFragment`, `MainFragment`) to take data from one source (e.g., a Race date from Jolpica) to query the other (e.g., find corresponding Session keys/details from OpenF1 using a date range). This is a critical, sometimes complex, interaction point.
3.  **Reactive Programming (Kotlin Flow):**
    *   **`StateFlow`:** Primarily used in ViewModels to expose UI state that Fragments observe.
    *   **`SharedFlow`:** Used for one-shot events like navigation triggers (`NavigationEvent` in `MainViewModel`).
    *   **Operators:** Expect heavy use of `map`, `combine`, `filterIsInstance`, `stateIn`, `collectLatest`, etc. Understanding these is key. `PositionsViewModel` is a good example of complex state combination.
4.  **Cache-First Strategy (`Resource` Wrapper):**
    *   Most repositories (`*Repository.kt`) follow this pattern:
        1.  Emit `Resource.Loading(true)`.
        2.  Query Room `DAO` and `emit(Resource.Success(cachedData))` if available.
        3.  Fetch from the `ApiService`.
        4.  On network success: Update Room `DAO`, re-query `DAO`, `emit(Resource.Success(freshData))`.
        5.  On network failure: `emit(Resource.Error(...))` only if no cache was emitted.
        6.  `finally { emit(Resource.Loading(false)) }` (Important!).
    *   This ensures a responsive UI even with network latency but adds complexity to repository logic.
5.  **Live Data & Polling:**
    *   ViewModels like `PositionsViewModel`, `LapsViewModel`, `CarDataViewModel` contain logic to poll OpenF1 endpoints at `Constants.POLLING_RATE` when `isLive` is true for a session.
    *   Polling uses coroutine `Job`s managed by fragment lifecycle methods (`onResume`, `onPause`, `onDestroyView`).
6.  **Dependency Injection (Hilt):**
    *   Dependencies (Repositories, DAOs, API Services, etc.) are provided via Hilt.
    *   See `di/AppModule.kt` for provider functions. Add new dependencies here.

**Contribution Workflow:**

1.  **Claim Issue:** Comment on an existing issue you want to tackle.
2.  **Branch:** `git checkout -b feature/your-feature` or `fix/bug-description` from `main`.
3.  **Code:**
    *   Adhere to the existing architecture and patterns described above.
    *   Follow the [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html).
    *   Use ViewBinding.
    *   Leverage Hilt for dependencies.
4.  **Commit:** Write clear, imperative [Conventional Commits](https://www.conventionalcommits.org/) (e.g., `feat: ...`, `fix: ...`).
5.  **Test:** Add Unit/Instrumentation tests where feasible. Ensure the app builds and runs.
6.  **Lint:** Run `./gradlew lintDebug` and fix issues.
7.  **Rebase:** Keep your branch updated: `git fetch origin && git rebase origin/main`.
8.  **Push & PR:** Push to your fork and open a Pull Request against the main repository's `main` branch.
    *   **PR Description:** Explain *what* and *why*. Link related issues (`Fixes #123`). Describe testing.

## 📜 License 📜

By contributing, you agree that your contributions will be licensed under the project's MIT License.

<details>
<summary>MIT License Text</summary>

```
MIT License

Copyright (c) 2024 Pankaj Kumar Roy

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

**Questions?** Open an [Issue](https://github.com/1719pankaj/onlyformula1/issues) with the `question` label.

Thanks for helping make OF1 even faster! 🚀
