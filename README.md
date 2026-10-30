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

## 🧠 1. Hasseena Autonomous Agent Mode

The flagship **Autonomous Agent** orchestrates complex, multi-step tasks end-to-end:

### Multi-Step Workflow
1. **Analyze Objective:** Receives high-level user commands (e.g. *"Perform a comprehensive hardware audit, research latest quantum computing developments, and synthesize an executive report"*).
2. **Intelligent Planning:** Evaluates the registered tool catalog and outputs a structured execution plan (max 10 steps).
3. **User Approval Gate:** Renders the planned steps, required tools, and parameters for user inspection.
4. **Sequential Execution:** Executes each operation sequentially, capturing live telemetry and real system responses.
5. **Self-Healing Dynamic Replanning:** If an environmental tool failure occurs, the agent automatically reasons and constructs an alternative step pathway (up to 3 replanning attempts).
6. **Executive Synthesis:** Compiles all collected data into an executive summary report with copy and text-to-speech triggers.

### Allowed Tool Catalog
| Tool Identifier | Description | Confirmation Required |
| :--- | :--- | :---: |
| `web_search` | Real-time Wikipedia & DuckDuckGo live web query | No |
| `deep_research` | Multi-query deep research with source attribution | No |
| `search_knowledge` | Local SQLite RAG knowledge base search | No |
| `save_memory` | Permanent fact/preference storage in long-term memory | No |
| `get_device_telemetry` | Real battery %, RAM, storage, network & thermal sensors | No |
| `set_flashlight` | Hardware camera torch control | No |
| `set_volume` | Android media audio stream adjustment | No |
| `set_brightness` | Screen brightness percentage adjustment | No |
| `launch_app` | Launches installed apps or Android system settings | No |
| `open_url` | Opens links in default web browser | No |
| `list_installed_apps` | Queries package manager for launcher applications | No |
| `read_active_notifications` | Reads status bar alerts via Notification Listener | No |
| `set_timer_alarm` | Dispatches system countdown timers or alarms | No |
| `place_phone_call` | Places outgoing phone call to contact or number | **Yes (Explicit)** |
| `compose_sms` | Prepares SMS text message to recipient | **Yes (Explicit)** |
| `synthesize_report` | Compiles gathered evidence into an executive report | No |

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

## 🧪 Testing & Verification

Comprehensive local JVM Robolectric unit tests verify all Critical User Journeys:
- **App Identity:** Confirms `HASASENA A1` launcher name and configuration.
- **Local Commands:** Verifies offline classification of battery, brightness, flashlight, and call intents without cloud calls.
- **Catalog Integrity:** Validates unique model IDs across all providers.
- **Autonomous Agent Registry:** Asserts all 16 registered tools and sensitive confirmation guards.
- **Real Tool Execution:** Tests device telemetry, volume, and brightness adjustments.
- **SQLite Persistence:** Validates inserting, querying, and deleting `AgentTask` and `AgentStep` records.

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
