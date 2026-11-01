# HASSEENA A1 — Autonomous Native AI Assistant for Android

**Hasseena A1** is an advanced, privacy-first, on-device autonomous AI companion and multi-step execution engine for Android. Designed with a dark cyberpunk sci-fi aesthetic, Hasseena A1 bridges cutting-edge large language models with native Android hardware controls, deep web research, real-time voice synthesis, interactive 3D female avatar visualizations, and structured multi-step autonomous agency.

---

## 🚀 Key Highlights & Architectural Principles

- **Direct Instant Access (Zero Login / Zero Sign-up Required):**
  - No Google Sign-In, email accounts, passwords, or authentication hurdles.
  - Launches directly to the mission-control dashboard upon first open, app restarts, or offline reboots.
  - Pure offline-first architecture powered by an embedded local SQLite database (`HasseenaDatabase`).
- **Real Autonomous Execution Engine (No Fake Delays or Mock Progress):**
  - Real tool dispatch to Android hardware, system services, and live search engines.
  - Honest status verification — steps are only marked `COMPLETED` when underlying native system APIs succeed.
- **Multi-Model Intelligence Router:**
  - Pluggable support for Gemini, Groq, OpenAI, Anthropic Claude, Perplexity, OpenRouter, NVIDIA NIM, and Mistral.
  - Automated tier routing: routes fast QA queries to lightweight low-latency models and complex architectural problems to reasoning engines.
- **Enterprise-Grade Privacy & Credential Security:**
  - API keys are protected in private device storage with Android Keystore encryption.
  - Zero external database sync required; sensitive actions (outgoing calls, SMS composition) require explicit user approval.

---

## 🧠 1. Six-in-One Autonomous AI Agent System (Mission Control)

The flagship **Autonomous Agent** operates as an integrated **Autonomous Mission Control** combining six distinct capability domains:

### The Six Integrated Autonomous Capabilities
1. **Tool 1: AI Coding & Project Editing (`AgentToolDomain.AI_CODING`):**
   - File inspection: `read_project_file`, `list_project_files`.
   - File creation, edits & refactoring: `write_project_file`, `propose_code_refactor`.
   - Safety rollback checkpoints: `rollback_file_checkpoint` integrated with `AutonomousCodingEngine`.
   - Changed files tracking with diff inspector and explicit user approval for destructive changes.
2. **Tool 2: Real Build & Test Executor (`AgentToolDomain.BUILD_TEST`):**
   - Genuine build verification: `run_build_check` evaluates Gradle DSL scripts, manifest structure, and compiler readiness.
   - Genuine unit test executor: `run_unit_tests` runs Robolectric JVM test suites and captures real test logs.
   - Diagnostic parser: `parse_compiler_log` identifies unresolved symbols and syntax issues, automatically routing verified errors to Self-Healing AI.
3. **Tool 3: Internet & Browser Automation (`AgentToolDomain.BROWSER_WEB`):**
   - Live research: `web_search` (Wikipedia & DuckDuckGo) and `deep_research` (parallel multi-query RAG synthesis).
   - Real HTTP GET documentation fetcher: `fetch_web_page` strips HTML, extracts clean text and verified hyperlinks.
   - Web form interaction: `interact_web_form` with explicit user confirmation for consequential actions.
4. **Tool 4: Android Phone Actions (`AgentToolDomain.PHONE_ACTIONS`):**
   - Accessibility screen reading: `accessibility_read_screen` inspects active window texts and buttons.
   - Accessibility controls: `accessibility_click`, `accessibility_input_text`, `accessibility_global_action` (BACK, HOME, RECENTS, NOTIFICATIONS, SCROLL).
   - Real-time device telemetry: `get_device_telemetry` (battery %, charging, RAM, storage, network, thermal).
   - Hardware controls: `set_flashlight`, `set_volume`, `set_brightness`, `launch_app`, `set_timer_alarm`.
   - Sensitive telephony: `place_phone_call`, `compose_sms` (gated by explicit user authorization).
5. **Tool 5: Persistent Memory & Task Queue (`AgentToolDomain.PERSISTENT_MEMORY`):**
   - Embedded SQLite persistence (`HasseenaDatabase` & `MemoryRepository`).
   - RAG document search: `search_knowledge`.
   - Long-term directives: `save_memory`.
   - Task queue states (`PENDING`, `PLANNING`, `RUNNING`, `PAUSED`, `WAITING_FOR_PERMISSION`, `COMPLETED`, `FAILED`, `CANCELLED`).
   - Pause, resume, retry, cancel, and sensitive credential scrubbing.
6. **Tool 6: AI Brain & Model API (`AgentToolDomain.AI_BRAIN`):**
   - Multi-model routing (Gemini, OpenAI, Claude, Local fallback) via `AIService`.
   - Multi-step task decomposition and step planning.
   - Executive synthesis: `synthesize_report` compiles gathered evidence into structured final answers.
   - Autonomous error reflection and replanning loops (up to 3 revisions).

### Allowed Tool Catalog
| Tool Identifier | Capability Domain | Description | Confirmation Required |
| :--- | :--- | :--- | :---: |
| `read_project_file` | Tool 1: AI Coding | Reads project source files with line count & token estimate | No |
| `write_project_file` | Tool 1: AI Coding | Writes file with pre-modification checkpoint | **Yes (Explicit)** |
| `list_project_files` | Tool 1: AI Coding | Scans project directories matching extensions | No |
| `propose_code_refactor` | Tool 1: AI Coding | Generates architectural diff patch for approval | **Yes (Explicit)** |
| `rollback_file_checkpoint`| Tool 1: AI Coding | Restores file to previous safety checkpoint | **Yes (Explicit)** |
| `run_build_check` | Tool 2: Build & Test | Verifies Gradle scripts, manifest, and compile readiness | No |
| `run_unit_tests` | Tool 2: Build & Test | Executes unit test suites and captures actual logs | No |
| `parse_compiler_log` | Tool 2: Build & Test | Parses compiler stack traces into Self-Healing AI | No |
| `web_search` | Tool 3: Browser & Web | Real-time Wikipedia & DuckDuckGo live web query | No |
| `deep_research` | Tool 3: Browser & Web | Multi-query deep research with source attribution | No |
| `fetch_web_page` | Tool 3: Browser & Web | Real HTTP GET fetcher with link extraction | No |
| `interact_web_form` | Tool 3: Browser & Web | Web interaction and query submission | **Yes (Explicit)** |
| `accessibility_read_screen`| Tool 4: Phone Actions| Reads visible texts and interactive buttons | No |
| `accessibility_click` | Tool 4: Phone Actions| Clicks UI element by label or description | No |
| `accessibility_input_text` | Tool 4: Phone Actions| Enters text into editable node via Accessibility | No |
| `accessibility_global_action`| Tool 4: Phone Actions| BACK, HOME, RECENTS, NOTIFICATIONS, SCROLL | No |
| `get_device_telemetry` | Tool 4: Phone Actions| Real battery %, RAM, storage, network & thermal sensors | No |
| `set_flashlight` | Tool 4: Phone Actions| Hardware camera torch control | No |
| `set_volume` | Tool 4: Phone Actions| Android media audio stream adjustment | No |
| `set_brightness` | Tool 4: Phone Actions| Screen brightness percentage adjustment | No |
| `launch_app` | Tool 4: Phone Actions| Launches installed apps or Android system settings | No |
| `open_url` | Tool 4: Phone Actions| Opens links in default web browser | No |
| `list_installed_apps` | Tool 4: Phone Actions| Queries package manager for launcher applications | No |
| `read_active_notifications`| Tool 4: Phone Actions| Reads status bar alerts via Notification Listener | No |
| `set_timer_alarm` | Tool 4: Phone Actions| Dispatches system countdown timers or alarms | No |
| `place_phone_call` | Tool 4: Phone Actions| Places outgoing phone call to contact or number | **Yes (Explicit)** |
| `compose_sms` | Tool 4: Phone Actions| Prepares SMS text message to recipient | **Yes (Explicit)** |
| `search_knowledge` | Tool 5: Memory & Queue| Local SQLite RAG knowledge base search | No |
| `save_memory` | Tool 5: Memory & Queue| Permanent fact/preference storage in long-term memory | No |
| `manage_mission_queue` | Tool 5: Memory & Queue| Lists, pauses, resumes, or cancels missions in queue | No |
| `synthesize_report` | Tool 6: AI Brain | Compiles gathered evidence into an executive report | No |

---

## 🎙️ 2. Voice Pipeline & Realistic 3D Avatar

- **3D Female Assistant Hologram:**
  - High-fidelity dual-monitor workstation rendering a sci-fi specialist avatar.
  - Ambient particle waves, dynamic head and camera tracking, and reactive visual pulses.
- **Arc Reactor Visualizer Orb:**
  - Concentric reactive rings animated by real-time microphone FFT frequency bands.
  - State pill indicators: `IDLE`, `LISTENING`, `THINKING`, `SPEAKING`, and `ERROR`.
- **Speech-to-Text & Barge-in:**
  - Android native `SpeechRecognizer` with streaming partial transcript updates.
  - One-tap or voice-activated barge-in interruption.
- **ElevenLabs Ultra-Realistic TTS:**
  - Integration with ElevenLabs voice synthesis API.
  - Custom voice pack assignments for distinct persona styling.

---

## 🛠️ 3. Native Android Controls & Telemetry

Hasseena A1 gives you full local control of your device without requiring third-party root:
- **Telemetry HUD:** Instant readout of battery percentage, charging state, battery temperature, available RAM, free internal storage, Wi-Fi SSID / cellular status.
- **Hardware Flashlight:** Toggles physical LED camera torch.
- **Volume & Display:** Media volume level sliders and manual/system display brightness controls.
- **System Intents:** Alarms, timers, calendar entries, and quick clipboard syncing.
- **Notification Listener Service:** Captures incoming status bar notifications for intelligent agent summarization.

---

## 🌐 4. Live Web Research & Local RAG Knowledge

- **Grounded Web Research:**
  - Concurrent multi-query engine querying Wikipedia and DuckDuckGo Instant Answers.
  - Zero fabricated citations — all sources contain real titles, verified URLs, and extracted evidence snippets.
- **Local RAG Knowledge Engine:**
  - Indexed document chunks stored in local SQLite database.
  - Keyword and semantic snippet retrieval with zero cloud latency.
- **Persistent Personal Memory:**
  - Extracts and remembers user directives, preferences, and custom instructions.

---

## 💻 5. Autonomous Coding Studio

- **Built-in Code Editor & Inspector:**
  - Multi-language syntax highlighting (Kotlin, Python, JavaScript, TypeScript, Bash, JSON, SQL).
  - Line numbers, cursor position, and syntax validation.
- **Autonomous Coding Engine:**
  - Generates full functions and scripts with zero hallucinations.
  - Self-repair debugging loops for automated code fixing.
  - Snapshot checkpoint history for instant rollback.

---

## 🗄️ 6. Local SQLite Architecture (`HasseenaDatabase`)

All app data is persisted completely on-device across 7 dedicated relational tables:
1. `chat_messages` — Full streaming conversation history with provider, latency, and tokens.
2. `memory_facts` — User preferences and long-term memory directives.
3. `response_cache` — Exact normalized response caching for sub-millisecond offline replies.
4. `automation_tasks` — Background scheduled routines and trigger rules.
5. `knowledge_items` — Indexed documents, markdown guidelines, and web links.
6. `agent_tasks` — Multi-step autonomous agent plans, objectives, and executive reports.
7. `agent_steps` — Granular step statuses, tool argument mappings, and execution logs.

---

## 📱 Navigation & App Layout

| Route ID | Label | Description |
| :--- | :--- | :--- |
| `home` | **Home** | Centerpiece Arc Reactor orb, quick actions, telemetry bar, and quick prompt launcher. |
| `autonomous_agent`| **Autonomous Agent** | Multi-step task planner, step cards, permission approval dialogs, and terminal logs. |
| `self_healing` | **Self-Healing AI** | Real-time error watchdog, root-cause diagnosis, code diff proposals, & rollback safety. |
| `chat` | **Chat** | Real-time streaming conversational interface with model routing badges. |
| `voice` | **Voice** | Dedicated full-screen voice assistant with wake word listening and barge-in. |
| `avatar_3d` | **3D Avatar** | Holographic 3D specialist workstation with reactive animations. |
| `coding` | **Coding** | Autonomous code generation, editor, terminal, and checkpoint manager. |
| `file_workspace` | **Files** | Storage Access Framework (SAF) workspace file manager and ZIP exporter. |
| `rag_knowledge` | **Knowledge** | Document library, chunk viewer, and search grounded in verified facts. |
| `research` | **Research** | Fast search and deep multi-source web intelligence synthesis. |
| `automation` | **Automation** | System task triggers, scheduled maintenance, and background routines. |
| `control_center`| **Controls** | System switches for torch, brightness, volume, and app launcher shortcuts. |
| `ai_providers` | **Providers** | API key management and latency benchmarking across 8 providers. |
| `settings` | **Settings** | Personality customization, memory wipes, ElevenLabs voices, and wake phrases. |

---

## 🛡️ 3. Hasseena Self-Healing AI System

A dedicated autonomous reliability watchdog that detects runtime anomalies, analyzes root causes, generates safe code patches with diff previews, and ensures zero-downtime recovery.

### Architectural Workflow
1. **Error Detection & Sanitization:**
   - Intercepts exceptions, timeouts, missing credentials, compilation failures, and agent step errors.
   - **Zero-Leakage Scrubbing:** Automatically redacts API keys (`sk-...`, `AIza...`), bearer tokens, and passwords using regex before database persistence or telemetry logging.
2. **Root-Cause Analysis (RCA):**
   - Dispatches error context to the AI diagnostic engine.
   - Formulates a structured diagnosis clearly distinguishing **CONFIRMED FACTUAL CAUSES** from **WORKING HYPOTHESES**.
   - Assesses recoverability, confidence rating (0–100%), blast radius, and recommended verification steps.
3. **Deterministic Auto-Recovery:**
   - Executes exponential backoff retries on transient network timeouts, socket exceptions, and rate limits (429/503).
   - Preserves coroutine cancellation boundaries.
4. **Safe Code Repair & Patch Management:**
   - Synthesizes minimal diffs (`CodeDiffPatch`) highlighting additions (`+`) and removals (`-`).
   - Requires explicit user authorization (`WAITING_FOR_APPROVAL`).
   - Creates a **Pre-Repair Safety Snapshot** before any source files are altered.
   - Applies the patch and runs automated verification.
   - **Verification Gate:**
     - **Verification PASSED:** Transitions to `RECOVERED` and resolves error record.
     - **Verification FAILED:** Immediately triggers an **Automatic Rollback** (`ROLLED_BACK`) to the pre-repair snapshot.
   - Allows users to inspect diffs, cancel proposals, or execute manual rollbacks at any time.

---

## 🧪 Testing & Verification

Comprehensive local JVM Robolectric unit tests verify all Critical User Journeys:
- **App Identity:** Confirms `HASASENA A1` launcher name and configuration.
- **Local Commands:** Verifies offline classification of battery, brightness, flashlight, and call intents without cloud calls.
- **Catalog Integrity:** Validates unique model IDs across all providers.
- **Autonomous Agent Registry:** Asserts all 16 registered tools and sensitive confirmation guards.
- **Real Tool Execution:** Tests device telemetry, volume, and brightness adjustments.
- **Autonomous Agent SQLite Persistence:** Validates inserting, querying, and deleting `AgentTask` and `AgentStep` records.
- **Self-Healing Credential Scrubbing:** Asserts that sensitive OpenAI and Gemini API keys are completely redacted to `[REDACTED_SECRET]`.
- **Self-Healing Exponential Backoff:** Verifies bounded retries and recovery on transient network errors.
- **Self-Healing Error Logging & Persistence:** Tests SQLite storage, query, and status updates of `SystemErrorRecord`.
- **Self-Healing Patch Lifecycle & Rollback:** Asserts state transitions through `WAITING_FOR_APPROVAL`, automatic rollback on verification failure, and successful `RECOVERED` state transitions.

Run tests:
```bash
gradle :app:testDebugUnitTest
```

---

## 📦 Build & Release

Compiled with Android Gradle Plugin 8.9.0 and Kotlin 2.1.0 targeting Android 15 (API 35):
```bash
gradle assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`
Saved debug APK: `HasseenaA1-Self-Healing-debug.apk`

