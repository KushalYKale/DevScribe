# DevScribe

DevScribe is a JavaFX-based desktop code editor with a built-in project launcher, file explorer, syntax highlighting, and an integrated terminal for running code. The app focuses on quick project onboarding (new, open, clone) and a lightweight editing experience.

## Features

- **Launcher for projects**: search, open, create, and clone repositories into a curated project list.
- **Multi-tab editor** with syntax highlighting for Java, Python, and C files.
- **Built-in terminal** to run Java, Python, JavaScript, and shell scripts directly from the editor.
- **Theme toggle** for light/dark UI modes.
- **Project tree** with lazy-loaded folders and quick double-click file open.
- **Productivity shortcuts**: `Ctrl+N`, `Ctrl+O`, `Ctrl+S`, `Ctrl+Shift+S`.

## Tech Stack

- **Java 21**
- **JavaFX 21**
- **RichTextFX** for the editor component
- **JGit** for repository cloning
- **Ikonli** for icon fonts

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+

### Run in Development

```bash
mvn javafx:run
```

### Build a Fat JAR

```bash
mvn package
```

The shaded JAR will be created by the Maven Shade plugin (see `target/`).

## Project Structure

```
src/main/java/com/DevScribe
├── App.java               # Application entry point
├── ui/screen              # Launcher/editor screens
├── editor                 # Editor + syntax highlighting
├── ui/dialogs             # Terminal + dialogs
├── utils                  # Utilities (paths, git, process streaming)
└── model                  # Data models
```

## Usage Notes

- The launcher stores recent projects in `projects.txt` in the app’s working directory.
- The terminal automatically compiles and runs Java files and can auto-install missing Python modules via pip.
- Files without an extension open a terminal shell.

## License

This project does not currently specify a license. If you plan to distribute it, add a license file.
