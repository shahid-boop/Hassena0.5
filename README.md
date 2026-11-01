# HASSEENA A1 --- Autonomous AI Assistant for Android

```{=html}
<p align="center">
```
`<strong>`{=html}A native Android AI assistant with chat, voice,
autonomous missions, coding tools, web research, device actions, local
memory, and self-healing workflows.`</strong>`{=html}
```{=html}
</p>
```
```{=html}
<p align="center">
```
`<a href="https://github.com/shahid-boop/Hassena0.5">`{=html}GitHub
Repository`</a>`{=html} ·
`<a href="https://github.com/shahid-boop/Hassena0.5/commits/main">`{=html}Commit
History`</a>`{=html}
```{=html}
</p>
```

------------------------------------------------------------------------

## Table of Contents

-   [Overview](#overview)
-   [Project Status and Accuracy](#project-status-and-accuracy)
-   [Main Features](#main-features)
-   [Six-in-One Autonomous Agent](#six-in-one-autonomous-agent)
-   [Self-Healing AI](#self-healing-ai)
-   [Chat, Voice, and Avatar](#chat-voice-and-avatar)
-   [Web Research and RAG Knowledge](#web-research-and-rag-knowledge)
-   [Coding Workspace and File Tools](#coding-workspace-and-file-tools)
-   [Android Device Controls](#android-device-controls)
-   [Memory, Database, and Task Queue](#memory-database-and-task-queue)
-   [Scrolling Improvements](#scrolling-improvements)
-   [Application Screens](#application-screens)
-   [Technology Stack](#technology-stack)
-   [Requirements](#requirements)
-   [Setup and Configuration](#setup-and-configuration)
-   [Build the APK](#build-the-apk)
-   [Run Tests](#run-tests)
-   [Permissions and Privacy](#permissions-and-privacy)
-   [Troubleshooting](#troubleshooting)
-   [Known Limitations](#known-limitations)
-   [Repository Structure](#repository-structure)
-   [Contributing](#contributing)
-   [License](#license)

------------------------------------------------------------------------

## Overview

**Hasseena A1** is a Kotlin-based native Android assistant project. It
combines an AI chat interface with voice interaction, an autonomous task
runner, project/file tools, web research, device assistance, persistent
local data, and a self-healing workflow.

The autonomous agent is designed to turn a user goal into a sequence of
steps, select supported tools, collect tool results, retain mission
status, and present a final summary. Sensitive actions should stop for
user approval rather than silently proceeding.

The app's package identity in the current Gradle configuration is:

  Property              Current project configuration
  --------------------- ---------------------------------------------------
  Application name      HASASENA A1 / Hasseena A1 (check launcher string)
  Application ID        `com.aistudio.applet.ztvtvv`
  Namespace             `com.example`
  Minimum Android SDK   API 24
  Compile SDK           API 36 (minor API level 1 configured)
  Target SDK            API 36
  Version name          `2.0`
  Version code          `1`
  Main language         Kotlin
  UI framework          Jetpack Compose

These values describe the checked project configuration and may change
in later commits.

## Project Status and Accuracy

This README documents the capabilities described in the repository and
the code changes visible in its commit history. **A feature being listed
here does not, by itself, prove that it works on every device or with
every provider.** Actual availability depends on implementation, Android
permissions, device support, configured API credentials, network access,
and test coverage.

Before distributing a build, run the build and tests described below and
manually verify the important flows on a real Android device. Report
failed, skipped, mocked, and unavailable tests separately.

## Main Features

-   AI chat interface with streaming responses.
-   Voice input using Android speech-recognition capabilities.
-   Text-to-speech support and an ElevenLabs integration path where
    configured.
-   Autonomous Mission Control with multi-step task planning and
    execution.
-   AI coding and project-file tools with checkpoints and rollback
    support.
-   Build/test diagnostics and compiler-log parsing.
-   Self-Healing AI workflow for error diagnosis, repair proposals,
    verification, and rollback.
-   Web search and HTTP page fetching for research.
-   Local knowledge retrieval using a SQLite-backed knowledge store.
-   Persistent mission queue and user-approved memory.
-   Android accessibility actions when the user enables the
    accessibility service.
-   Device information and supported controls such as flashlight,
    volume, brightness, app launching, and timers.
-   User confirmation gates for consequential actions such as calls,
    messages, form submission, and destructive file changes.
-   Chat and mission scrolling improvements, including a
    scroll-to-bottom control in chat.

Some functions require an API provider, a network connection, an Android
system service, or explicit user permission.

------------------------------------------------------------------------

## Six-in-One Autonomous Agent

The Autonomous Agent brings six tool domains together in Mission
Control.

### 1. AI Coding and Project Editing

The tool registry describes operations for inspecting and changing
project files.

Capabilities include:

-   `read_project_file` --- read a project file and provide a bounded
    preview.
-   `list_project_files` --- list files matching supported paths,
    extensions, or filters.
-   `write_project_file` --- create or update a permitted file, with a
    pre-edit checkpoint where supported.
-   `move_project_file` --- relocate a permitted project file with
    safety handling.
-   `propose_code_refactor` --- prepare a code-change proposal/diff for
    review.
-   `rollback_file_checkpoint` --- restore a file from a recorded
    checkpoint.
-   `analyze_code_diagnostics` --- perform available source diagnostics
    such as basic syntax-balance and import checks.
-   Changed-file tracking --- record modified paths for review in
    Mission Control.

File access should remain inside approved project/workspace locations.
Review proposed changes before applying risky edits, and keep an
independent backup of important work.

### 2. Build and Test Executor

The build/test domain helps inspect Android project configuration and
diagnose failures.

Documented tools include:

-   `run_build_check` --- inspect relevant Gradle and manifest
    configuration.
-   `run_unit_tests` --- run the supported local unit/Robolectric test
    workflow when the execution environment allows it.
-   `parse_compiler_log` --- extract compiler diagnostics and route
    suitable failures to the self-healing workflow.
-   `verify_apk_artifact` --- inspect an APK artifact when the required
    file and tooling are available.

A configuration check is not the same as a successful Gradle build. A
test report should only say a test passed when the test actually ran and
returned a successful result.

### 3. Internet and Browser Automation

The web domain supports research and page retrieval using the providers
and HTTP functionality configured in the project.

Documented operations include:

-   `web_search` --- search through configured search sources.
-   `deep_research` --- gather and synthesize information across
    multiple queries.
-   `fetch_web_page` --- retrieve public page content over HTTP and
    extract readable text/links.
-   `interact_web_form` --- support form-related interactions with
    confirmation gates for consequential submissions.

Network requests can fail because of timeouts, rate limits, blocked
pages, redirects, authentication requirements, or provider quotas.
Search snippets should not be represented as if the full page was
retrieved. The agent must not bypass authentication, CAPTCHAs, or
website security controls.

### 4. Android Phone Actions

Supported actions depend on Android version, device hardware, and
granted permissions.

Documented tools include:

-   `accessibility_read_screen` --- inspect visible accessibility text
    and controls.
-   `accessibility_click` --- activate a supported accessibility node.
-   `accessibility_input_text` --- enter text into an editable
    accessibility node.
-   `accessibility_global_action` --- invoke supported global actions
    such as Back, Home, Recents, or scrolling.
-   `get_device_telemetry` --- read available device information such as
    battery, charging, storage, network, or memory.
-   `set_flashlight` --- control the camera torch on compatible
    hardware.
-   `set_volume` --- adjust a supported audio stream.
-   `set_brightness` --- adjust brightness where Android
    permissions/settings permit it.
-   `launch_app` --- open a supported installed app or system activity.
-   `open_url` --- open a URL in the default browser.
-   `list_installed_apps` --- list launchable apps subject to Android
    package-visibility rules.
-   `read_active_notifications` --- read notifications only after the
    user grants Notification Access.
-   `set_timer_alarm` --- request a supported timer or alarm action.
-   `place_phone_call` and `compose_sms` --- prepare communication
    actions with explicit user confirmation.

These tools do not provide unrestricted control of Android. Protected
system settings, locked screens, background restrictions, device policy,
and permission denial may prevent an operation.

### 5. Persistent Memory and Task Queue

The memory domain stores task state and selected knowledge locally using
the project's database/repository layer.

Documented capabilities include:

-   Save and retrieve mission records and execution steps.
-   Maintain task states such as `PENDING`, `PLANNING`, `RUNNING`,
    `PAUSED`, `WAITING_FOR_PERMISSION`, `COMPLETED`, `FAILED`, and
    `CANCELLED`.
-   Pause, resume, retry, or cancel eligible tasks where implemented.
-   Search the local knowledge store with `search_knowledge`.
-   Save user-approved information with `save_memory`.
-   Track task results and modified files.
-   Redact recognized credentials from selected logs/outputs.

Memory is not a substitute for a backup. Users should be able to review
stored information and should avoid saving passwords, API keys,
authentication tokens, or other secrets as ordinary memories.

### 6. AI Brain and Model Routing

The AI domain interprets user goals, helps plan steps, and summarizes
tool results.

The repository describes integrations or routing paths for providers
such as Gemini, OpenAI, Groq, Anthropic Claude, Mistral, Perplexity,
OpenRouter, and NVIDIA NIM. **A provider is usable only when the
corresponding integration is implemented, its model is supported, and
valid credentials/configuration are supplied.** Do not assume every
provider is enabled in every build.

Documented agent behavior includes:

-   Break a request into manageable steps.
-   Select tools from the registered tool catalog.
-   Track step status and returned results.
-   Reflect on certain errors and re-plan within bounded limits.
-   Produce a final summary through `synthesize_report`.
-   Handle provider failures, missing credentials, quotas, and network
    errors where the relevant error handling is implemented.

An AI-generated statement is not proof that a tool operation succeeded.
Mission completion should be based on actual tool results, exit codes,
database state, or another observable outcome.

------------------------------------------------------------------------

## Self-Healing AI

The Self-Healing module is intended to help diagnose failures and manage
safe recovery.

### Workflow

1.  **Detect:** Receive an eligible compiler, network, tool, or runtime
    failure.
2.  **Sanitize:** Remove recognized secrets from diagnostic text before
    saving or displaying it.
3.  **Diagnose:** Analyze available error details and identify likely
    causes.
4.  **Propose:** Prepare a targeted fix or code diff when code repair is
    supported.
5.  **Approve:** Ask the user before applying changes that require
    approval.
6.  **Checkpoint:** Preserve a reversible snapshot before modifying a
    file where supported.
7.  **Apply and verify:** Apply the approved change and run the
    available validation.
8.  **Recover or roll back:** Mark the issue resolved only if
    verification succeeds; otherwise restore the prior state when
    rollback is available.
9.  **Record:** Keep a repair outcome for later inspection.

Self-healing is bounded by available diagnostics, provider access,
permissions, and the checks that can actually run. It cannot guarantee
that every bug will be repaired. If verification is unavailable or a
repair fails, the app should report that honestly and stop safely.

------------------------------------------------------------------------

## Chat, Voice, and Avatar

### Chat

-   Conversational interface with streaming-response support where
    available.
-   Message actions such as copy, share, speak, or retry where
    implemented.
-   Model/provider indicators where available.
-   A floating **Scroll to Bottom** button when the message list is not
    at the bottom.

### Voice

-   Android speech-recognition integration for voice input.
-   Partial transcript updates where supported by the active recognizer.
-   Voice output through configured Android TTS and/or external voice
    providers.
-   ElevenLabs voice synthesis integration path where credentials and
    configuration are available.
-   Listening, thinking, speaking, idle, and error states where
    implemented.

Voice recognition and synthesis depend on the device's speech services,
network, provider configuration, microphone permission, and quota. Do
not assume continuous wake-word detection is active unless it is
explicitly configured and tested.

### 3D Avatar

The project README describes a sci-fi assistant/avatar presentation with
animated visual feedback. Rendering quality and interactivity depend on
the implementation included in the current source tree and the device's
graphics performance.

------------------------------------------------------------------------

## Web Research and RAG Knowledge

### Web research

-   Run supported web queries.
-   Fetch public webpage content using the configured HTTP client.
-   Extract readable text and links from retrieved pages.
-   Combine findings from multiple queries for deeper research.
-   Present source URLs so users can inspect the original material.

Retrieved web content is untrusted input. It must not be allowed to
override the app's safety rules, authorize privileged operations, or
trigger sensitive actions without user approval.

### Local RAG knowledge

The local knowledge workflow is intended to:

1.  Store supported knowledge items or document content.
2.  Break content into searchable pieces where implemented.
3.  Retrieve relevant local snippets for a query.
4.  Use retrieved context to ground an answer.

Retrieval quality depends on what has actually been imported and
indexed. Do not claim that a document has been indexed unless the
operation reports success.

------------------------------------------------------------------------

## Coding Workspace and File Tools

The coding/workspace features are intended to support source inspection
and controlled project edits.

-   Browse supported workspace files.
-   Read and preview text files.
-   Edit or create permitted files.
-   Inspect proposed code diffs.
-   Keep checkpoints before supported edits.
-   Review the changed-file list after a mission.
-   Roll back eligible changes from checkpoint history.
-   Inspect APK artifacts when the required tools are available.

Only work on files that the user has authorized. Keep an external copy
of the original project before allowing an autonomous agent to perform
large edits.

------------------------------------------------------------------------

## Android Device Controls

The app declares permissions and services for features such as
microphone input, network access, camera/flashlight use, contacts or
calendar-related actions, notifications, calls, and accessibility
assistance.

Depending on the specific feature, the user may need to:

-   Grant a runtime permission.
-   Enable the app's Accessibility Service in Android Settings.
-   Enable Notification Access in Android Settings.
-   Allow display-over-other-apps access for a floating assistant
    overlay.
-   Enable microphone access for voice input.
-   Configure a compatible AI or speech provider.

Only grant access needed for the feature you want to use. Android may
block an action even after a permission is granted, depending on OS
version, device manufacturer, background restrictions, or security
policy.

------------------------------------------------------------------------

## Memory, Database, and Task Queue

The project uses local persistence for conversation-related data,
knowledge, tasks, and agent execution state. The repository has used a
SQLite-backed `HasseenaDatabase`/repository approach; inspect the
current schema before changing it.

The database may contain records for:

-   Chat messages.
-   User-approved memory facts.
-   Response cache entries.
-   Automation tasks.
-   Knowledge items.
-   Agent tasks and steps.
-   Error records and self-healing repair history.

**Database migration rule:** never use destructive table drops or clear
app storage simply to make an upgrade pass. Add safe migrations,
preserve existing records, and test upgrades from the previous schema.
Back up important user data before installing development builds.

------------------------------------------------------------------------

## Scrolling Improvements

The repository includes a commit titled
`refactor(ui): improve scroll behavior and list padding`.

Changes in that commit include:

-   `AutonomousAgentScreen.kt`
    -   Avoid repeatedly resetting the list when the current task
        step/status changes.
    -   Scroll to the top when a newly observed task ID is detected.
    -   Add extra bottom content padding so the final mission content is
        easier to reach.
-   `ChatStreamScreen.kt`
    -   Separate message-list changes from token-stream updates.
    -   Avoid competing animated scroll calls during streaming.
    -   Keep the list pinned to the bottom while generating only when
        the user is already near the bottom.
    -   Add a floating **Scroll to Bottom** control.
    -   Add test tags for the chat list and scroll control.
-   `HomeVoiceScreen.kt`
    -   Add bottom spacing to reduce overlap with the system navigation
        area.

These are source-level changes. Confirm the behavior on a real device
with short and long conversations, streaming responses, the keyboard
open, and different screen sizes.

------------------------------------------------------------------------

## Application Screens

The README and navigation code describe screens or routes for the
following areas. Exact availability should be checked against the
current navigation implementation.

  -----------------------------------------------------------------------
  Screen / Area                       Purpose
  ----------------------------------- -----------------------------------
  Home / Voice Home                   Assistant home, quick prompts,
                                      voice entry, and status information

  Chat                                Text conversation and streaming
                                      responses

  Autonomous Agent / Mission Control  Plan, execute, and inspect
                                      multi-step missions

  Self-Healing AI                     View errors, repair proposals, and
                                      recovery outcomes

  Coding                              Code-generation and source-editing
                                      workflow

  File Workspace                      Browse and manage permitted
                                      workspace files

  Research                            Search and summarize public web
                                      sources

  Knowledge / RAG                     Search locally stored knowledge

  Voice                               Voice input and speech output
                                      controls

  3D Avatar                           Assistant visual presentation

  Controls                            Supported device controls

  Providers                           Configure supported AI providers
                                      and credentials

  Automation                          Task/automation entry points where
                                      implemented

  Settings                            App preferences and supported
                                      assistant settings
  -----------------------------------------------------------------------

A screen name in this table does not guarantee every control on that
screen is complete. Verify each button and action in the current build.

------------------------------------------------------------------------

## Technology Stack

The checked project configuration includes:

-   **Kotlin** --- primary application language.
-   **Jetpack Compose** --- declarative Android UI.
-   **Android Gradle Plugin** --- Android build system via
    version-catalog configuration.
-   **Kotlin serialization** --- JSON serialization support.
-   **Kotlin Coroutines** --- asynchronous work.
-   **OkHttp and Retrofit** --- HTTP/network operations.
-   **Room/SQLite-related dependencies** --- local persistence
    components.
-   **Android SpeechRecognizer / Text-to-Speech** --- device speech
    services where used.
-   **Robolectric and JUnit** --- local unit/UI-adjacent testing.
-   **Android Accessibility Service** --- user-enabled UI assistance.

For exact dependency versions, consult `gradle/libs.versions.toml`,
`app/build.gradle.kts`, and the Gradle wrapper files in the repository.

------------------------------------------------------------------------

## Requirements

-   Android Studio or another compatible Gradle/Android build
    environment.
-   A compatible JDK for the configured Android Gradle Plugin and Kotlin
    toolchain.
-   Android SDK components required by the project's `compileSdk`.
-   Internet access to download dependencies and to use cloud AI/web
    services.
-   Provider API credentials for any external AI or voice service that
    requires them.
-   A physical Android device or emulator for runtime verification.
-   Additional Android permissions for voice, accessibility,
    notifications, overlay, calls, or other protected features.

The project sets `minSdk` to API 24 and `targetSdk` to API 36 in the
current Gradle configuration.

------------------------------------------------------------------------

## Setup and Configuration

### 1. Clone the repository

``` bash
git clone https://github.com/shahid-boop/Hassena0.5.git
cd Hassena0.5
```

Open the project root in Android Studio and allow Gradle sync to finish.

### 2. Configure secrets safely

The project configures the Gradle Secrets plugin to read `.env` and use
`.env.example` as its default-properties file.

1.  Inspect `.env.example` and the provider configuration code to
    identify the exact variable names used by this revision.
2.  Create a local `.env` file if the project workflow requires it.
3.  Add only the credentials needed for the providers you intend to use.
4.  Keep `.env`, private keystores, and real credentials out of Git.
5.  Never paste API keys into public issues, README files, source code,
    APK assets, or chat messages.

Do not invent environment-variable names: use the exact names documented
by `.env.example` and the implementation. A provider listed in the UI
may still require valid credentials and a supported model ID.

### 3. Configure AI providers

In the app, open the provider/settings area if available and configure
the provider credentials and model selection supported by the current
build. Confirm connectivity with a small test request before running a
long autonomous mission.

If you see an environment-variable prompt, identify the missing variable
from the build output or `.env.example`; do not enter a secret into an
unrelated field.

### 4. Enable optional Android services

Enable only the services needed for the feature you want to test:

-   Accessibility Service for screen reading and UI actions.
-   Notification Access for reading notifications.
-   Microphone permission for speech recognition or voice mode.
-   Overlay permission for a floating assistant orb, if used.
-   Other runtime permissions only when prompted by a feature.

------------------------------------------------------------------------

## Build the APK

From the repository root, use the Gradle wrapper if it is present:

``` bash
./gradlew :app:assembleDebug
```

On Windows:

``` bat
gradlew.bat :app:assembleDebug
```

If the project environment does not include the wrapper but has a
compatible Gradle installation, the equivalent task is:

``` bash
gradle :app:assembleDebug
```

Expected debug APK location:

``` text
app/build/outputs/apk/debug/app-debug.apk
```

The build must finish successfully and the output file must exist and be
non-empty before calling it a successful APK build.

### Release builds and signing

The current Gradle configuration reads release signing information from
environment variables and a keystore path. A release build may fail if
the keystore or required credentials are missing.

-   Never commit a signing keystore or its passwords.
-   Do not publish a release APK signed with a temporary/debug key as if
    it were the production release.
-   Android normally cannot install an APK as an update over an existing
    app if the signing certificate does not match.
-   Verify package ID, version, signing certificate, and
    installation/update behavior before distribution.

------------------------------------------------------------------------

## Run Tests

Run the local unit test suite:

``` bash
./gradlew :app:testDebugUnitTest
```

Windows:

``` bat
gradlew.bat :app:testDebugUnitTest
```

If the wrapper is unavailable but Gradle is installed:

``` bash
gradle :app:testDebugUnitTest
```

### Recommended manual verification checklist

-   [ ] App builds from the current source.
-   [ ] App starts and main navigation opens.
-   [ ] Chat sends a prompt and displays the response.
-   [ ] Chat can scroll up and down during a long response.
-   [ ] Scroll-to-bottom button appears and works when appropriate.
-   [ ] Mission Control does not jump unexpectedly during step updates.
-   [ ] Mission creation, cancellation, and persistence behave
    correctly.
-   [ ] Coding tools respect allowed file paths and create checkpoints.
-   [ ] Rollback restores the intended file version.
-   [ ] Self-Healing reports the actual result of a repair attempt.
-   [ ] Web research shows real sources and handles network failures.
-   [ ] Provider errors and quota limits are displayed clearly.
-   [ ] Permission-denied cases fail safely.
-   [ ] Existing database data survives an app upgrade.
-   [ ] APK metadata and signing are checked before distribution.

A test suite passing does not replace device testing. Clearly
distinguish real provider/device tests from tests using mocks or
Robolectric.

------------------------------------------------------------------------

## Permissions and Privacy

Hasseena A1 requests or declares permissions for certain features.
Depending on the installed revision, these can include network state,
internet, microphone, camera, contacts, calendar, phone calls,
notifications, Bluetooth, accessibility, notification access, and
display overlays.

Privacy recommendations:

-   Use only trusted AI providers and review their privacy terms.
-   Do not store secrets in long-term memory or mission descriptions.
-   Redact credentials from logs and error reports.
-   Treat web pages and retrieved documents as untrusted content.
-   Require confirmation before sending messages, placing calls,
    submitting consequential forms, deleting data, or applying risky
    source changes.
-   Keep a backup before autonomous file editing or database migrations.
-   Disable optional services when you no longer need them.

Accessibility and notification access can expose sensitive on-screen
content. Enable them only if you understand and want the associated
functionality.

------------------------------------------------------------------------

## Troubleshooting

### Gradle sync or build fails

-   Read the first actionable error in the Gradle output.
-   Confirm that the configured JDK and Android SDK are installed.
-   Check network access and dependency resolution.
-   Run the relevant task again after fixing the root cause; do not
    treat a configuration scan as a successful build.

### AI requests fail

-   Check that the selected provider is supported by the current
    implementation.
-   Verify the credential and model ID.
-   Check connectivity, provider quota, rate limits, and billing
    requirements.
-   Avoid repeatedly retrying authentication or quota errors.

### Voice does not work

-   Grant microphone permission.
-   Confirm a speech-recognition service is installed and available.
-   Check the selected TTS/voice provider and its credentials.
-   Test network-dependent voice synthesis separately from offline
    device speech.

### Accessibility actions do not work

-   Enable the app's accessibility service in Android Settings.
-   Make sure the target screen exposes accessibility nodes.
-   Remember that protected system UI, secure screens, and some apps may
    block actions.

### Notifications are unavailable

-   Enable Notification Access in Android Settings.
-   Confirm that the relevant notification is still present and
    accessible.
-   Do not assume the service can read notifications before access is
    granted.

### APK will not update an installed version

-   Confirm the application ID.
-   Check whether the APK signing certificate matches the installed app.
-   Check version code and installation error output.
-   Do not uninstall the existing app until you have backed up any
    important local data.

### Scrolling still feels wrong

-   Test with the keyboard open and closed.
-   Check both a short chat and a long streaming response.
-   In Mission Control, observe whether the list jumps when a step
    changes state.
-   Capture logs and device/Android version information before changing
    scroll logic again.

------------------------------------------------------------------------

## Known Limitations

-   External AI providers need valid configuration, connectivity, and
    available quota.
-   Provider names in documentation do not guarantee every model is
    currently integrated or enabled.
-   Android device actions depend on OS restrictions, hardware, and
    user-granted permissions.
-   Accessibility and notification features require explicit
    system-level enablement.
-   Web content may be blocked, incomplete, or rate-limited.
-   RAG answers depend on the documents and knowledge items actually
    stored and indexed.
-   Self-healing cannot guarantee a repair and must report unverified
    outcomes honestly.
-   Build and test results can vary by environment; only executed
    results should be reported.
-   A debug APK may not be compatible with updating an app signed with a
    different certificate.

Please update this section as limitations are confirmed or resolved by
actual testing.

------------------------------------------------------------------------

## Repository Structure

Representative paths in the current repository include:

``` text
Hassena0.5/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/example/
│       │       ├── MainActivity.kt
│       │       ├── models/
│       │       ├── providers/
│       │       ├── services/
│       │       │   ├── accessibility/
│       │       │   ├── agent/
│       │       │   ├── healing/
│       │       │   └── memory/
│       │       └── ui/features/
│       │           ├── agent/
│       │           ├── chat/
│       │           ├── healing/
│       │           └── home/
│       └── test/
├── app/build.gradle.kts
├── settings.gradle.kts
├── gradle/
├── .env.example
└── README.md
```

This is a representative map, not a guarantee that every listed folder
or file exists in every branch. Check the repository tree for the exact
current layout.

------------------------------------------------------------------------

## Contributing

1.  Open an issue describing the bug or feature.
2.  Reproduce the issue and include Android version, device/emulator
    details, and relevant sanitized logs.
3.  Keep changes focused and avoid unrelated rewrites.
4.  Add or update tests for the behavior changed.
5.  Run the relevant build and test tasks.
6.  Do not include API keys, keystores, personal data, or private logs
    in a commit.
7.  Describe what was actually tested and what remains unverified.

For autonomous-agent changes, include the expected tool behavior,
permission requirements, failure path, and rollback behavior.

## License

Add the project's actual license here after confirming that a `LICENSE`
file exists and identifying its terms. Until then, do not assume that
the repository is licensed under MIT, Apache-2.0, or another open-source
license.

------------------------------------------------------------------------

## Current Change Reference

The repository history includes a UI scrolling improvement commit
affecting:

-   `app/src/main/java/com/example/ui/features/agent/AutonomousAgentScreen.kt`
-   `app/src/main/java/com/example/ui/features/chat/ChatStreamScreen.kt`
-   `app/src/main/java/com/example/ui/features/home/HomeVoiceScreen.kt`
-   `HasseenaA1-Mission-Control-debug.apk`

See the [scroll behavior
commit](https://github.com/shahid-boop/Hassena0.5/commit/07e6d54a890b00a2609b57bc258b9aed6eed9b2b)
for the exact patch.

The Self-Healing and agent-domain work is documented in the
[agent/self-healing
commit](https://github.com/shahid-boop/Hassena0.5/commit/67d7f982cf889ecf56ba8dd4f52dbb404d41750e).

------------------------------------------------------------------------

**Hasseena A1 --- build carefully, verify real behavior, protect user
data, and report limitations honestly.**
