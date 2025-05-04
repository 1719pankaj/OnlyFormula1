# Contributing to OF1 - OnlyFormula1 App 🏎️💨

First off, HUGE thanks for considering contributing to OF1! 🙌 Whether you're fixing a bug, proposing a new feature, or diving into the code, your help is incredibly valuable. This project thrives on community energy, and we're stoked to have you potentially join the pit crew!

This document provides guidelines to make contributing as smooth as hitting the perfect apex. Let's get those tires warm! 🔥

## 🚦 How Can I Contribute?

There are several ways you can help push OF1 forward:

*   **🐞 Reporting Bugs:** If you find something glitchy or not working as expected, please let us know!
*   **💡 Suggesting Enhancements:** Got a killer idea for a new feature or an improvement to an existing one? We're all ears!
*   **💻 Writing Code:** Ready to get your hands dirty? Tackle an existing issue or propose your own enhancement via a Pull Request.
*   **📄 Improving Documentation:** See something unclear in the README or code comments? Help us make it better!

## 🛠️ Getting Started: Setup & Running

Ready to fire up the engine? Here’s how to get the project running locally:

1.  **Prerequisites:**
    *   **Android Studio:** Make sure you have a recent version installed (Project targets Android API 35). [Download here](https://developer.android.com/studio)
    *   **Git:** Essential for version control.
    *   **SDKs:** Ensure you have Android SDK Platform 35 installed via the Android Studio SDK Manager. The project `minSdk` is 31.
2.  **Fork & Clone:**
    *   Fork the repository on GitHub.
    *   Clone your fork locally:
        ```bash
        git clone https://github.com/YOUR_USERNAME/onlyformula1.git
        cd onlyformula1
        ```
3.  **Open in Android Studio:**
    *   Open the cloned project directory in Android Studio.
    *   Let Gradle sync and download dependencies (this might take a moment).
4.  **Debug Keystore (Signing Config):**
    *   The project uses the standard Android debug keystore by default (`~/.android/debug.keystore`). For most local development, Android Studio manages this automatically, and you shouldn't need to do anything.
    *   If you have a custom debug keystore setup or use environment variables (`DEBUG_STORE_FILE`, etc.), ensure they are configured correctly in your system if needed. Usually, this isn't necessary for just building/running debug variants.
5.  **Build & Run:**
    *   Build the project (Build > Make Project).
    *   Run the `app` configuration on an emulator (API 31+) or a physical device.

You should now see the splash screen followed by the main race list!

## 🐛 Reporting Bugs

Found a puncture in the code? Help us patch it up!

1.  **Check Existing Issues:** Search the [GitHub Issues](https://github.com/1719pankaj/onlyformula1/issues) first to see if your bug has already been reported.
2.  **Create a New Issue:** If it's a new bug, create a detailed issue report:
    *   **Title:** Clear and descriptive (e.g., "Crash when selecting race from 1955 in dialog").
    *   **Description:**
        *   **Steps to Reproduce:** Crucial! Be precise.
        *   **Expected Behavior:** What *should* have happened?
        *   **Actual Behavior:** What *did* happen?
        *   **Screenshots/Videos:** Extremely helpful if applicable.
        *   **Logs:** Include relevant Logcat output, especially stack traces for crashes.
        *   **Environment:** Device model, Android version, OF1 App Version (`versionName` from `app/build.gradle.kts`).

## ✨ Suggesting Enhancements

Have an idea for the next pit stop strategy or a slick new feature?

1.  **Check Existing Issues/Discussions:** See if your idea is already being discussed.
2.  **Create a New Issue:**
    *   **Title:** Clear and descriptive (e.g., "Feature Request: Add driver standings comparison chart").
    *   **Description:** Explain the feature, why it would be valuable, and how you envision it working. Mockups or examples are welcome!
    *   Use the "enhancement" label if available.

## 💻 Code Contributions: The Rules of the Road

Ready to jump in the driver's seat? Follow these guidelines:

1.  **Claim an Issue:** If you want to work on an existing issue, comment on it first to let others know and prevent duplicated effort.
2.  **Branching:**
    *   Create a new branch from the `main` branch for your feature or bugfix:
        ```bash
        git checkout main
        git pull origin main # Make sure you have the latest main
        git checkout -b feature/your-awesome-feature # or fix/bug-description
        ```
3.  **Coding Style:**
    *   Follow the standard [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html).
    *   Adhere to Android development best practices used in the project:
        *   **MVVM:** Maintain the separation of concerns. UI logic in Fragments, business/data logic in ViewModels, data operations in Repositories.
        *   **ViewBinding:** Use ViewBinding for interacting with XML layouts.
        *   **Coroutines & Flow:** Utilize `StateFlow` for UI state, `SharedFlow` for events, and coroutines for background work.
        *   **Resource Class:** Wrap data fetched from repositories in the `Resource` sealed class (`Loading`, `Success`, `Error`).
        *   **Hilt:** Use Hilt for dependency injection. Add new dependencies via Hilt modules (`di/AppModule.kt`).
        *   **Immutability:** Prefer `val` over `var` where possible. Use immutable collections (`List`, `Map`) for state exposed to the UI.
4.  **Commit Messages:**
    *   Write clear and concise commit messages.
    *   Use the imperative mood (e.g., "Fix crash..." not "Fixed crash...").
    *   Consider using Conventional Commits format (e.g., `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`). Example:
        ```
        feat: Add race status indicator bar to main list items

        Introduces a vertical bar on the left of each race card
        in the main list to visually represent its status (Past, Live/Next, Future).
        Updates colors and adapter logic.
        ```
5.  **Testing (Highly Encouraged!):**
    *   While the project might not have extensive tests *yet*, adding relevant unit tests (for ViewModels, helpers) or instrumentation tests (for UI interactions, Fragments) with your contribution is *massively* appreciated!

## 🚀 Pull Request (PR) Process

Ready to merge your changes back onto the main track?

1.  **Ensure Build Success:** Make sure your code builds successfully without errors (`./gradlew build`).
2.  **Linting:** Run the linter (`./gradlew lintDebug`) and fix any reported issues.
3.  **Rebase (Optional but Recommended):** Keep your feature branch up-to-date with the main branch:
    ```bash
    git fetch origin
    git rebase origin/main
    # Resolve any conflicts
    ```
4.  **Push:** Push your feature branch to your fork:
    ```bash
    git push origin feature/your-awesome-feature
    ```
5.  **Open a PR:** Go to the original OF1 repository on GitHub and open a Pull Request from your fork's feature branch to the `main` branch.
6.  **PR Description:**
    *   Provide a clear title.
    *   Describe the changes made and *why*.
    *   Link to the relevant GitHub issue(s) using `Fixes #123` or `Closes #123`.
    *   Explain how to test your changes.
7.  **Review:** A maintainer will review your PR. Be prepared to discuss your changes and make adjustments based on feedback.
8.  **Merge:** Once approved, your contribution will be merged! 🎉

## 🤝 Code of Conduct

Let's keep the paddock friendly! While we don't have a formal Code of Conduct document *yet*, please adhere to standard open-source etiquette: Be respectful, constructive, and collaborative in all interactions (issues, PRs, discussions).

## 🤔 Questions?

Got questions about contributing or the codebase? The best place to ask is by opening an [Issue](https://github.com/1719pankaj/onlyformula1/issues) with the "question" label.

---

**Thanks again for your interest in contributing to OF1! Let's build the fastest F1 data app together!** 🏆