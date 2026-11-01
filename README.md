# AUTO CLICKER PRO MAX

**Auto Clicker Pro Max** is a complete, enterprise-grade cross-platform automation ecosystem consisting of a native Android automation suite and a responsive Web Automation Studio.

---

## Ecosystem Architecture

### 1. Native Android Application
- **Safe Automation DSL (`com.example.dsl`)**:
  - Sandboxed parser, AST representation, and bounded interpreter.
  - Supports: `VAR`, `CONST`, `SET`, `TAP`, `DOUBLE_TAP`, `LONG_PRESS`, `SWIPE`, `SCROLL`, `WAIT`, `REPEAT`, `FOR`, `IF/ELSE`, `GROUP`, `FUNCTION`, `CALL`, `STOP`, `PAUSE`.
  - Execution bound enforcement (10,000 steps budget to prevent runaway loops).
  - Diagnostics with exact line and column numbers.
  - Code auto-formatter.
- **Script Studio Pro**:
  - Code editor with line numbers, syntax error detection, auto-complete chips, Undo/Redo history, and a step-by-step debugger with a live variable inspection drawer.
- **Visual Script Builder**:
  - Modular drag-and-drop block interface (Tap, Swipe, Wait, Repeat, etc.) with bidirectional conversion between visual blocks and safe DSL scripts.
- **AI Script Assistant**:
  - Natural language understanding that parses user intentions (e.g., *"Tap center location 100 times every 50ms"*, *"Swipe down every 3 seconds 20 times"*).
  - Generates structured execution plans, human-readable explanations, and verified DSL scripts.
  - Mandatory user review & approval gate before execution.
- **Script Marketplace**:
  - Rich template library with categories (*Auto Tap*, *Multi Click*, *Auto Scroll*, *Swipe Automation*, *Productivity*, *Repetitive Tasks*).
  - Search, ratings, download metrics, verified badges, live script preview, and one-click import.
- **Cloud Synchronization & Device Manager**:
  - Cross-platform backup and restore.
  - Multi-device connectivity monitoring (Android Host, Web Automation Studio, Tablet nodes).
  - Offline mode and conflict resolution.
- **Web Automation Studio (In-App & Standalone)**:
  - Embedded HTML5/React/Tailwind responsive Web Studio inside Android via WebView (`app/src/main/assets/web/index.html`) and as standalone deployable web assets (`/web/index.html`).
- **Automation Laboratory (Workspace)**:
  - Interactive virtual touchscreen canvas with real-time visual click ripples.
  - Movable, customizable floating control panel with gradient/solid/icon-tint styling, live counter, and emergency stop.
- **Native Accessibility Engine**:
  - Built on Android `AccessibilityService` (`dispatchGesture`) for authorized clicks, swipes, and multi-touch gestures.
  - Transparent disclosure dialog adhering strictly to Google Play developer policies.

---

### 2. Web Automation Studio (`/web/index.html`)
- Complete responsive Single Page Application.
- Features:
  - **Script Studio**: Interactive DSL code editor with syntax formatting and validation.
  - **Visual Workflow Builder**: Visual flow connecting action blocks.
  - **AI Assistant**: Natural language synthesizer.
  - **Marketplace**: Searchable community template library.
  - **Sandbox Simulator**: Virtual DOM arena for touch and gesture simulation.
  - **Cloud Sync**: Device manager and backup manager.

---

## DSL Grammar Specification

```ebnf
Script         ::= (Statement | NEWLINE)* EOF ;
Statement      ::= TapStmt | DoubleTapStmt | LongPressStmt | SwipeStmt | ScrollStmt
                 | WaitStmt | PauseStmt | StopStmt | VarDecl | VarAssign
                 | RepeatStmt | ForStmt | IfStmt | GroupStmt | FnDef | FnCall ;

TapStmt        ::= "TAP" Expr Expr [Expr] ;
DoubleTapStmt  ::= "DOUBLE_TAP" Expr Expr ;
LongPressStmt  ::= "LONG_PRESS" Expr Expr Expr ;
SwipeStmt      ::= "SWIPE" Expr Expr Expr Expr [Expr] ;
ScrollStmt     ::= "SCROLL" ("UP" | "DOWN") [Expr] ;
WaitStmt       ::= "WAIT" Expr ;
PauseStmt      ::= "PAUSE" Expr ;
StopStmt       ::= "STOP" ;
VarDecl        ::= ("VAR" | "CONST") IDENTIFIER "=" Expr ;
VarAssign      ::= "SET" IDENTIFIER "=" Expr ;
RepeatStmt     ::= "REPEAT" Expr "{" Statement* "}" ;
ForStmt        ::= "FOR" IDENTIFIER "FROM" Expr "TO" Expr "{" Statement* "}" ;
IfStmt         ::= "IF" Expr "{" Statement* "}" ["ELSE" "{" Statement* "}"] ;
GroupStmt      ::= "GROUP" STRING_LITERAL "{" Statement* "}" ;
FnDef          ::= "FUNCTION" IDENTIFIER "()" "{" Statement* "}" ;
FnCall         ::= "CALL" IDENTIFIER "()" ;
```

---

## Build & Test Instructions

### Android Build
```bash
# Compile and generate Debug APK
gradle assembleDebug

# Run Unit and Robolectric Tests
gradle :app:testDebugUnitTest
```

APK Output Location:
`app/build/outputs/apk/debug/app-debug.apk`

### Web Studio Deployment
Deploy the contents of `/web/` to any static web host, CDN, or Node.js server.
