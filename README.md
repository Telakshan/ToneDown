# ToneDown

ToneDown is an AI-powered profanity muter that processes video files to automatically detect and silence explicit language. It uses OpenAI's Whisper (via `whisper-jni`) for transcription and `ffmpeg` (via `jaffree`) for media processing.

## Prerequisites

- **Java 11+**
- **Maven**
- **FFmpeg**: Must be installed and available in your system PATH.

## Features

- **Drag & Drop Interface**: Easily process videos by dragging them into the application.
- **AI Transcription**: Uses a local Whisper model to transcribe and identify profanity.
- **Smart Muting**: Automatically mutes the audio during detected profanity with precise timing.
- **Verification**: Includes a verification step to ensure the output is clean.
- **Formats**: Supports MP4, MOV, and WAV.

## Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/Telakshan/ToneDown.git
    cd ToneDown
    ```

2.  Build the project using Maven:
    ```bash
    mvn clean install
    ```

## Usage

1.  Run the application:
    ```bash
    mvn javafx:run
    ```

2.  **Download Model**: Upon first run, the application will download the necessary AI model (`ggml-base.en.bin`) to `~/.tonedown/models/`.

3.  **Process a Video**:
    - Drag and drop a video file onto the window.
    - Click "Start Processing".
    - The application will extract audio, transcribe it, mute profanity, and save the result as `tonedown_<filename>`.

## Development

- **Build**: `mvn clean package`
- **Test**: `mvn test`

## Project Structure

- `src/main/java`: Source code.
- `src/main/resources`: Assets and FXML/CSS.
- `src/test/java`: Unit tests.
