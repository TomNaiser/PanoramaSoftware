# Short manual

[Overview and features](../../README.md) · [Detailed German handbook](../README.md)

This guide covers firmware **2.4.75**, Android **0.6.25** and Windows **27.4.4**. Use matching V2 versions; the old projects are not a description of the current workflow.

## What you need

A compatible motorized pan/tilt head with working position encoders, a supported electrical camera trigger and a Bluetooth serial connection. The current firmware development target is Arduino Nano R4 with AS5048B encoders. Camera/lens balance, movement limits and the tilt reference must match the physical setup. Android requires Android 8.0 or newer; the Windows application requires .NET Framework 4.8.

The software controls the head and triggers the camera. Images stay on the camera; image transfer and stitching are handled separately.

## 1. Connect

Pair the head in the operating system's Bluetooth settings. In Android, open the **Device** page (`Gerät`), grant the requested Bluetooth permissions, select the paired head and connect. On Windows, select the serial COM port assigned to the Bluetooth connection.

Wait for fresh device information, measured positions, reference status and limits. Use one client at a time. Reconnecting reads the current device state; it does not automatically restart a previous move or acquisition. Check the connection-loss policy before a run: stopping and continuing after link loss are different configured behaviors.

## 2. Frame and plan

Use **Manual** (`Manuell`) to position the camera. Held arrows jog the head; target and step moves provide more precise framing. Store the desired start and end positions after the head has settled.

On **Plan**, choose the camera/sensor and focal length, overlap, movement mode and capture timing. Inspect the graphical preview and confirm that all targets fit within the actual device limits.

| Mode | Capture pattern |
|---|---|
| Grid / raster | Tilt rows within each pan column |
| Spherical | Tilt bands with different numbers of pan positions |

Spherical mode does not extend the hardware's travel range or guarantee full zenith/nadir coverage. Settle time, shutter-pulse duration and exposure wait are separate settings. Exposure wait must suit the real camera exposure and capture sequence; the camera does not report completion back to this software.

## 3. Load, then start

Transfer the plan and wait until the head confirms the matching loaded plan. **Loading does not start the panorama.** Select **Start** explicitly, then monitor the **Run** page (`Lauf`). Firmware performs movement, settling checks and triggering; the client records progress, positions and warnings.

The display distinguishes targets, measured positions and completed captures. A command acknowledgement is not a measured arrival. Small position warnings and safety faults have different meanings.

## 4. Pause, stop and recover

| Situation | What to do |
|---|---|
| Normal pause | Wait until the head confirms the paused state, then choose Resume |
| Stop | Outputs stop immediately; no automatic parking move follows. Use the appropriate recovery workflow before another run |
| Fault | Read the reason, correct the cause, acknowledge the incident and wait for valid, fresh device state |

**Android:** a saved interruption can be continued by preparing a new remaining job. Choose the affected or previous complete column for a grid. For spherical acquisition, recovery retains the original bands and may repeat an earlier band to preserve the original targets. Load the proposed continuation, wait for confirmation, then start it separately. The linked capture archive retains the earlier captures and repetitions.

An ongoing sensor fault, changed reference or other unresolved device condition can block recovery. The app displays the blocking reason; acknowledgement alone does not make the head ready.

**Windows:** normal pause/resume is available, but Android's newer checkpoint/remaining-job workflow has not yet been ported. Device recovery may clear the loaded job. Do not assume the same fault-recovery choices are available in both clients.

## 5. Save and export

On Android, **Extras** provides saved runs, Papywizard XML/CSV export and automatic-backup status. Captures are archived in SQLite. Complete XML snapshots are updated in the background during acquisition; they can be rebuilt from the archive after an app restart. After an interruption, check the backup status or export again before transferring the file to the stitching computer. The app's storage is not a separate external backup.

On Windows, captures are stored in a journal. The classic live XML writer closes the document at normal completion; use the separate journal-snapshot export when a run was interrupted.

Import the positions together with the camera images into your stitching workflow. Capture counts and electrical trigger confirmations do not prove that the camera saved every image. Repeated columns may also produce additional camera files. Reconstructed entries can have unknown measured positions; they are not new encoder measurements.

## Other useful controls

Both clients offer camera profiles, column navigation, configurable capture timing and timelapse. Review the selected run mode and settings before starting. Android additionally exposes the current recovery and backup status. More options and their restrictions are covered in the [German operating guide](../02-bedienung-und-acquise.md).

## Related projects

- [PanoDuinoControl](https://github.com/TomNaiser/PanoDuinoControl): firmware for the head.
- [PanoramaControl](https://github.com/TomNaiser/PanoramaControl): Windows client.
- [PanoBoard](https://github.com/TomNaiser/PanoBoard): related electronics designs; use the wiring for your actual board revision.
- [Gigapixel-Tools](https://github.com/TomNaiser/Gigapixel-Tools): separate vignetting-correction and PTGui tile-arrangement utilities.

## Build from source

Clone this repository with `--recurse-submodules`. Published application packages, when available, belong to their matching release; the commands below build from source.

**Android:** JDK 17 or a compatible Android Studio JBR, Android SDK 35 and the included Gradle wrapper. From the repository root:

```powershell
./PanoramaAndroid/gradlew.bat -p PanoramaAndroid testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to `PanoramaAndroid/app/build/outputs/apk/debug/`. Installing an APK built with a different signing identity is not an in-place update of an existing installation.

**Windows:** .NET 8 SDK for host tests and .NET Framework 4.8 build tools for the application:

```powershell
dotnet run --project PanoramaControlGitRepo/Tests/ProtocolTests/ProtocolTests.csproj
dotnet msbuild "PanoramaControlGitRepo/PanoramaControl/PanoramaControl Version 27.csproj" /t:Build /p:Configuration=Release /p:GenerateManifests=false
```

**Firmware:** Arduino CLI, `arduino:renesas_uno` 1.6.0 and the AS5048B sensor library. The R4 build script accepts local tool/library paths as parameters:

```powershell
pwsh -File PanoduinoControlGitRepo/Tests/build-r4.ps1 -ToolDirectory '<local-tool-directory>' -SensorLibraryRoot '<local-library-directory>'
```

Building and uploading are separate operations. See the [detailed build guide](../10-build-tests-und-wartung.md) for toolchain setup, host tests and upload parameters.

For current feature differences, mechanical limits and remaining field validation, see the [feature matrix](../08-funktionsstand.md) and [known limitations](../09-probleme-und-roadmap.md). [License coverage](../../LICENSES.md) applies to the respective components.
