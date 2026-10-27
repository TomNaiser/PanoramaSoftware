# PanoramaSoftware

**Automated multi-row and spherical panorama acquisition for an encoder-controlled pan/tilt camera head.**

[Short manual](docs/en/manual.md) · [Deutscher Einstieg](README.de.md) · [Ausführliche deutsche Dokumentation](docs/README.md)

Plan a panorama from your camera, lens, coverage and overlap, send it to the head, and let the firmware handle positioning, settling and shutter timing. Android and Windows provide the controls; Papywizard XML records capture positions for an external stitching workflow.

## Highlights

- **Plan before you shoot.** Camera/lens profiles, overlap settings and a graphical preview for rectangular grids and spherical bands.
- **Run the sequence on the head.** Firmware controls the movement and capture sequence after the plan has been loaded. Loading and starting are separate actions.
- **Use encoder feedback.** Measured pan/tilt positions, settling checks and visible position warnings help assess what actually happened.
- **Recover interrupted work.** Android and Windows can prepare a continuation from a selected grid column or original spherical band, with completed captures retained in the archive.
- **Keep useful data after an interruption.** Android and Windows save captures in SQLite and update complete Papywizard XML snapshots in the background, with rebuilding after an app restart.
- **Stay in control.** Manual target moves, held-arrow jogging, pause/resume, column navigation, timelapse and an accessible stop action.

## Projects and workflow

| Project | Role |
|---|---|
| [Android app](PanoramaAndroid/README.md), documented version **0.6.27** | Mobile planning, acquisition, recovery and export; German/English interface; Android 8.0 or newer |
| [PanoDuinoControl](https://github.com/TomNaiser/PanoDuinoControl), **2.4.75** | Confirmed firmware baseline; Nano R4 with AS5048B encoders |
| [PanoramaControl](https://github.com/TomNaiser/PanoramaControl), **27.5.4** | Windows client using .NET Framework 4.8 |
| [PanoBoard](https://github.com/TomNaiser/PanoBoard) | Related controller-board design files |
| [Gigapixel-Tools](https://github.com/TomNaiser/Gigapixel-Tools) | Separate Python tools for vignetting correction and arranging loose tiles in PTGui projects |

Capture with this software, export the positions, and stitch the camera images in an external application. Gigapixel-Tools provides optional post-processing utilities; it is a separate project. The [hardware background](https://sites.google.com/site/thomasnaiser/home/gigapixel-hardware) describes the earlier builds.

## Get started

1. Use a matching V2 firmware/client combination and check the head's reference and movement limits.
2. Pair over Bluetooth and connect from **one** client: Android or Windows.
3. Check the camera profile and timing, set the endpoints, and inspect the preview.
4. Load the plan, wait for confirmation, then explicitly start it.
5. Monitor progress and export Papywizard XML/CSV. See the [short manual](docs/en/manual.md) for recovery and export details.

Firmware and Windows are Git submodules. To include them when cloning this repository:

```text
git clone --recurse-submodules <repository-url>
```

Build requirements and commands are included in the [short manual](docs/en/manual.md). The detailed [German handbook](docs/README.md) also covers architecture, protocol, functions, parameters and diagrams.

## Current limits

As of 27 October 2026, Windows 27.5.4 and stored motor profile **revision 40** have completed **96 measured captures**, including a 72-capture run and a reverse-direction check. Maximum pan error was **0.236°**, with a **0.089° median** across all 96 captures. The longer run retained 13 target-missed warnings; maximum tilt error was 0.315°. Firmware 2.4.75 and Android 0.6.27 remain unchanged, and revision 39 is retained as a fallback. See the [Pan correction and validation report](docs/validation/2026-10-27-pan-positioning.md) in German.

Windows includes checkpoint-based remaining jobs and continuous complete XML export. Real recovery after a new fault, the complete Surface workflow and changing load or wind still need validation. Spherical planning is limited by the head's actual mechanical range. Capture confirmations record electrical triggers and positions, not successful camera-file storage or image sharpness; see the [feature matrix](docs/08-funktionsstand.md) and [known limitations](docs/09-probleme-und-roadmap.md).

## License

The Android app and original contents of this main repository use the [MIT License](LICENSE), Copyright (c) 2026 TomNaiser. Firmware, Windows and third-party components retain their respective licenses; see [license coverage and notices](LICENSES.md).
