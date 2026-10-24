# Quellcode-Referenz

[Inhalt](../README.md) · [Funktionssignaturen](funktionen.md)

Aktive Firmware-Quellen, Android-Produktionsklassen und im Windows-Version-27-Projekt eingebundene C#-Dateien; generierte Designer-/AssemblyInfo-Dateien und Tests ausgenommen. Ein eingebundener Legacy-Helfer ist dadurch nicht automatisch der aktive V2-Pfad. Die Fachkapitel erklären die tatsächlich verwendeten Verantwortungen.

Windows/Firmware-Quelllinks zeigen auf ihre eigenen GitHub-Repositories (`master`). Der lokale Dateipfad bleibt im Text erkennbar. Für publizierte Releases Links auf erreichbare Commitstände fixieren; lokale unpublizierte Änderungen werden auf GitHub erst nach deren Veröffentlichung sichtbar.

## Firmware

| Datei | Deklarierte Typen | Erfasste Signaturen |
|---|---|---:|
| [BenchDrive.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h) | `BenchDrive`, `BenchDriveSample`, `BenchDriveStatus`, `BenchOutputStatus` | 13 |
| [BufferedPanTrace.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h) | `BufferedPanTrace`, `Sample` | 11 |
| [DebugEventLog.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/DebugEventLog.h) | `DebugEventCodeV2`, `DebugEventLogV2`, `DebugEventRecordV2` | 7 |
| [MotionConfigStore.cpp](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.cpp) | Freie Funktionen / Definitionen | 4 |
| [MotionConfigStore.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.h) | `IMotionConfigStorage`, `MotionConfigStore`, `StoredMotionConfig` | 8 |
| [MotionConfiguration.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfiguration.h) | `AxisMotionConfiguration` | 2 |
| [MotionController.cpp](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp) | Freie Funktionen / Definitionen | 43 |
| [MotionController.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h) | `Axis`, `AxisMotionStatus`, `MotionAxisPhase`, `MotionController`, `MotionDriveStage`, `MotionProfile`, `MotionTraceSnapshot` | 48 |
| [MotionParameters.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionParameters.h) | Freie Funktionen / Definitionen | 0 |
| [MotionSettings.cpp](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.cpp) | Freie Funktionen / Definitionen | 3 |
| [MotionSettings.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.h) | `MotionParameterDescriptor`, `MotionParameterId`, `MotionSettings` | 8 |
| [PanoDuinoControl.ino](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino) | `BoardMotionConfigStorage`, `PanoHardwareAdapter` | 62 |
| [PanoramaExecutor.cpp](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp) | Freie Funktionen / Definitionen | 23 |
| [PanoramaExecutor.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h) | `IExecutorEvents`, `IPanoramaHardware`, `MotionDiagnosticsV2`, `MotionTelemetryV2`, `PanoramaExecutor`, `RunSegmentV2` | 85 |
| [PanoramaPlanV2.cpp](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.cpp) | Freie Funktionen / Definitionen | 7 |
| [PanoramaPlanV2.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.h) | `PanoramaPlanV2`, `TargetPositionV2` | 6 |
| [PanStopModel.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanStopModel.h) | `PanStopModel` | 1 |
| [ProtocolMessages.cpp](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolMessages.cpp) | Freie Funktionen / Definitionen | 2 |
| [ProtocolMessages.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolMessages.h) | `DisconnectPolicyV2`, `RunPhaseV2`, `RunStateV2` | 2 |
| [ProtocolV2.cpp](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp) | Freie Funktionen / Definitionen | 23 |
| [ProtocolV2.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h) | `IV2FrameHandler`, `ProtocolV2`, `V2Frame` | 27 |
| [SafetyConfiguration.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyConfiguration.h) | Freie Funktionen / Definitionen | 0 |
| [SafetyController.cpp](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp) | `BootRecord`, `StoredTiltLimits`, `StoredTiltZero` | 17 |
| [SafetyController.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h) | `SafetyController`, `SafetyResult` | 20 |
| [SerialTransport.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h) | `SerialTransport` | 11 |
| [StackWatermark.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/StackWatermark.h) | `StackWatermark` | 6 |
| [TargetArrivalMonitor.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/TargetArrivalMonitor.h) | `TargetArrivalMonitor` | 10 |
| [V2Controller.cpp](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp) | Freie Funktionen / Definitionen | 45 |
| [V2Controller.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h) | `ManualCaptureRecordV2`, `ManualKind`, `ManualResult`, `V2Controller` | 50 |
| [VersionInfo.h](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/VersionInfo.h) | Freie Funktionen / Definitionen | 0 |

## Android

| Datei | Deklarierte Typen | Erfasste Signaturen |
|---|---|---:|
| [BluetoothSession.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java) | `BluetoothSession`, `Listener` | 14 |
| [CameraProfile.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CameraProfile.java) | `CameraProfile` | 3 |
| [CaptureQualityGuard.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureQualityGuard.java) | `CaptureQualityGuard`, `Issue` | 3 |
| [CaptureStabilitySettings.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStabilitySettings.java) | `CaptureStabilitySettings` | 5 |
| [CaptureStore.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java) | `CaptureStore`, `Checkpoint`, `PlotSnapshot`, `Run` | 35 |
| [CaptureWarning.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureWarning.java) | `CaptureWarning` | 3 |
| [CommandEffects.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CommandEffects.java) | `CommandEffects` | 1 |
| [DeviceLink.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceLink.java) | `DeviceLink` | 5 |
| [DeviceSession.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceSession.java) | `DeviceSession` | 2 |
| [DeviceState.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java) | `DeviceState` | 14 |
| [EndpointSelection.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/EndpointSelection.java) | `EndpointSelection` | 3 |
| [FrameReader.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/FrameReader.java) | `FrameReader` | 1 |
| [IncidentTracker.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/IncidentTracker.java) | `IncidentTracker` | 4 |
| [JogHold.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/JogHold.java) | `JogHold` | 4 |
| [LivePapywizard.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java) | `BeforeReplace`, `Listener`, `LivePapywizard`, `Publisher`, `Snapshot`, `Source` | 14 |
| [MainActivity.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java) | `MainActivity` | 91 |
| [ManualActionProgress.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualActionProgress.java) | `ManualActionProgress`, `Outcome` | 2 |
| [ManualActionStatus.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualActionStatus.java) | `ManualActionStatus` | 4 |
| [ManualMotionOptions.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualMotionOptions.java) | `ManualMotionOptions` | 2 |
| [ManualMoveProgress.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualMoveProgress.java) | `ManualMoveProgress` | 2 |
| [ManualTargets.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualTargets.java) | `ManualTargets` | 2 |
| [P2Codec.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/P2Codec.java) | `Frame`, `P2Codec` | 6 |
| [PanoramaPlan.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java) | `PanoramaPlan` | 11 |
| [PanoramaService.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java) | `LocalBinder`, `Observer`, `PanoramaService` | 91 |
| [PlanAbort.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanAbort.java) | `PlanAbort` | 6 |
| [PlanPreview.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java) | `EndpointListener`, `PlanPreview` | 40 |
| [PlotViewport.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotViewport.java) | `PlotViewport` | 8 |
| [PlotWorkspace.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotWorkspace.java) | `PlotWorkspace` | 3 |
| [RasterRecovery.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RasterRecovery.java) | `RasterRecovery` | 6 |
| [ReconnectPolicy.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ReconnectPolicy.java) | `ReconnectPolicy` | 3 |
| [RecoveryFlow.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RecoveryFlow.java) | `RecoveryFlow` | 5 |
| [RestPreparation.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RestPreparation.java) | `RestPreparation` | 6 |
| [RunPlotModel.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunPlotModel.java) | `Capture`, `RunPlotModel`, `Sample` | 7 |
| [RunRecoveryPause.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunRecoveryPause.java) | `RunRecoveryPause` | 3 |
| [StatusLogPolicy.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/StatusLogPolicy.java) | `StatusLogPolicy` | 1 |
| [SupportLog.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/SupportLog.java) | `SupportLog` | 5 |
| [V2Client.java](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java) | `Pending`, `Result`, `Transport`, `V2Client` | 19 |

## Windows

| Datei | Deklarierte Typen | Erfasste Signaturen |
|---|---|---:|
| [DiagnosticSessionLog.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/DiagnosticSessionLog.cs) | `DiagnosticSessionLog` | 5 |
| [ManualMoveTargets.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/ManualMoveTargets.cs) | `ManualMoveTargets` | 6 |
| [PanoramaPlanFactory.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaPlanFactory.cs) | `PanoramaPlanFactory` | 3 |
| [PanoramaSessionController.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs) | `ConnectionStateEventArgs`, `DeviceCapabilitiesEventArgs`, `DeviceDiagnosticsEventArgs`, `DeviceInfoEventArgs`, `DeviceStatusEventArgs`, `PanoramaSessionController`, `RunEventEventArgs`, `SessionMessageEventArgs` | 71 |
| [PanoramaSessionController.Operations.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs) | `CaptureWarningEventArgs`, `PanoramaSessionController` | 28 |
| [PapyWizardRunExporter.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs) | `IPapyWizardWriter`, `LegacyPapyWizardWriter`, `PapyWizardRunExporter` | 11 |
| [RunArchive.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs) | `LocalSettings`, `RunArchive` | 19 |
| [RunTimingSettings.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunTimingSettings.cs) | `RunTimingSettings` | 3 |
| [CalculationsHelper.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/CalculationsHelper.cs) | `CalculationsHelper` | 1 |
| [BluetoothComTransport.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/BluetoothComTransport.cs) | `BluetoothComTransport` | 9 |
| [ConnectionState.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/ConnectionState.cs) | `ConnectionState` | 0 |
| [ISerialTransport.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/ISerialTransport.cs) | `ISerialTransport`, `SerialLineReceivedEventArgs`, `TransportErrorEventArgs`, `TransportStateChangedEventArgs` | 6 |
| [AngleMdeg.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/AngleMdeg.cs) | `AngleMdeg` | 8 |
| [CoordinateContract.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/CoordinateContract.cs) | `CoordinateContract` | 4 |
| [DeviceCapabilities.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/DeviceCapabilities.cs) | `DeviceCapabilities` | 1 |
| [DeviceDiagnostics.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/DeviceDiagnostics.cs) | `DeviceDiagnostics` | 0 |
| [DeviceInfo.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/DeviceInfo.cs) | `DeviceInfo` | 0 |
| [DeviceStatus.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/DeviceStatus.cs) | `DeviceStatus` | 1 |
| [PanoramaPlan.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/PanoramaPlan.cs) | `PanoramaPlan`, `TargetPosition` | 7 |
| [RunEvent.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/RunEvent.cs) | `RunEvent` | 0 |
| [RunSegment.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/RunSegment.cs) | `RunSegment` | 0 |
| [RunSegmentResolver.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/RunSegmentResolver.cs) | `RunSegmentResolver` | 1 |
| [RunState.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/RunState.cs) | `DisconnectPolicy`, `MoveMode`, `RunPhase`, `RunState` | 0 |
| [SessionFeatures.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs) | `CaptureWarnings`, `ManualActionProgress`, `ManualActionStatus`, `PositionTrace`, `Sample`, `SessionFeatures` | 14 |
| [SphericalGeometry.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SphericalGeometry.cs) | `SphericalGeometry` | 4 |
| [Form1.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs) | `BufferedPanel`, `PanoControlForm` | 96 |
| [Form1.Current.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs) | `NavigationArrowButton`, `PanoControlForm` | 52 |
| [InterfaceClassAcquisition2UI.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/InterfaceClassAcquisition2UI.cs) | `InterfaceClassAcquisition2UI` | 0 |
| [muControllerInterface.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs) | `muControllerInterface` | 19 |
| [PanoControlApp.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoControlApp.cs) | `PanoControlApp` | 1 |
| [PanoramaAcquisitionClass.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs) | `PanoramaAcquisitionClass` | 11 |
| [PapyWizardHelper.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PapyWizardHelper.cs) | `PapyWizardHelper` | 5 |
| [Params.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Params.cs) | `Params` | 4 |
| [CommandResult.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/CommandResult.cs) | `CommandResult` | 1 |
| [Crc16Ccitt.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/Crc16Ccitt.cs) | `Crc16Ccitt` | 2 |
| [ProtocolFrame.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolFrame.cs) | `FrameDecodeError`, `ProtocolFrame` | 1 |
| [ProtocolFrameCodec.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolFrameCodec.cs) | `ProtocolFrameCodec` | 4 |
| [ProtocolV2Client.cs](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs) | `PendingCommand`, `ProtocolErrorEventArgs`, `ProtocolFrameEventArgs`, `ProtocolTraceEventArgs`, `ProtocolV2Client` | 17 |
