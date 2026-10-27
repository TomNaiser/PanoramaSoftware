# Funktions- und Methodensignaturen

[Inhalt](../README.md) · [Dateien und Typen](quellcode.md)

Automatisch lexikalisch erfasste Deklarationen/Definitionen aus den Produktionsdateien. Die Listen dienen zum Auffinden im Code und ergänzen die erklärten Funktionen der Fachkapitel. Enthalten sind auch interne Helfer sowie durch Buildflags eingeschlossene Varianten. Headerdeklaration und Implementierung können beide erscheinen.

Kein Compiler-API-Dump: komplexe Operatoren, manche Konstruktor-Initialisierungen, Properties ohne Parameter und anonyme/lambda-basierte Handler können fehlen. Es wird keine stabile öffentliche ABI behauptet. Variable-/Datenmodellreferenz und vollständiger Parameterkatalog sind getrennt dokumentiert.

## Firmware

### PanoduinoControlGitRepo/PanoDuinoControl/BenchDrive.h

- [L35](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L35): `BenchDrive()`
- [L40](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L40): `void configureLimits(int32_t panMin, int32_t panMax, int32_t tiltMin, int32_t tiltMax)`
- [L43](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L43): `bool start(bool tilt, int pwm, uint32_t duration, int32_t travel, uint32_t now, int32_t pan, int32_t tiltPosition, bool fresh)`
- [L59](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L59): `bool startCruise(bool tilt, int pwm, uint32_t duration, int32_t travel, uint32_t now, int32_t pan, int32_t tiltPosition, bool fresh)`
- [L69](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L69): `void tick(uint32_t now, int32_t pan, int32_t tilt, bool valid, bool enabled)`
- [L114](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L114): `void stop(uint32_t now, const char *reason)`
- [L116](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L116): `bool active() const`
- [L117](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L117): `int drive(bool tilt) const`
- [L118](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L118): `BenchDriveStatus status() const`
- [L120](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L120): `bool sample(uint16_t index, BenchDriveSample &value) const`
- [L124](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L124): `bool inside(int32_t pan,int32_t tilt) const`
- [L126](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L126): `static int64_t distance(int32_t a,int32_t b)`
- [L128](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BenchDrive.h#L128): `void append(uint32_t now,int32_t pan,int32_t tilt)`

### PanoduinoControlGitRepo/PanoDuinoControl/BufferedPanTrace.h

- [L12](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h#L12): `BufferedPanTrace():_head(0),_count(0),_overwritten(0),_invalid(0),_lastMs(0), _previousMs(0),_previousPosition(0),_recording(false),_ready(false),_previous(false),_badTiming(false)`
- [L14](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h#L14): `void sample(uint32_t now,int32_t position,int16_t pwm,bool enabled,bool valid)`
- [L30](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h#L30): `bool ready() const`
- [L31](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h#L31): `unsigned int count() const`
- [L32](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h#L32): `uint32_t overwritten() const`
- [L33](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h#L33): `uint32_t invalid() const`
- [L34](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h#L34): `bool badTiming() const`
- [L35](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h#L35): `const Sample &at(unsigned int index) const`
- [L36](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h#L36): `uint16_t checksum() const`
- [L47](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h#L47): `static void addByte(uint16_t &crc,uint8_t byte)`
- [L51](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/BufferedPanTrace.h#L51): `void append(int32_t position,int16_t pwm,uint16_t delta)`

### PanoduinoControlGitRepo/PanoDuinoControl/DebugEventLog.h

- [L60](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/DebugEventLog.h#L60): `DebugEventLogV2()`
- [L62](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/DebugEventLog.h#L62): `void clear()`
- [L69](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/DebugEventLog.h#L69): `void append(const DebugEventRecordV2 &source)`
- [L88](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/DebugEventLog.h#L88): `uint8_t count() const`
- [L89](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/DebugEventLog.h#L89): `uint16_t dropped() const`
- [L90](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/DebugEventLog.h#L90): `uint16_t newestSequence() const`
- [L94](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/DebugEventLog.h#L94): `const DebugEventRecordV2 &record(uint8_t offset) const`

### PanoduinoControlGitRepo/PanoDuinoControl/MotionConfigStore.cpp

- [L13](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.cpp#L13): `uint32_t MotionConfigStore::checksum(const StoredMotionConfig &r)`
- [L25](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.cpp#L25): `bool MotionConfigStore::valid(const StoredMotionConfig &r)`
- [L34](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.cpp#L34): `bool MotionConfigStore::load(MotionSettings &settings,uint32_t &revision)`
- [L44](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.cpp#L44): `bool MotionConfigStore::save(const MotionSettings &settings,uint32_t revision)`

### PanoduinoControlGitRepo/PanoDuinoControl/MotionConfigStore.h

- [L20](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.h#L20): `virtual ~IMotionConfigStorage()`
- [L21](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.h#L21): `virtual bool read(uint8_t slot,StoredMotionConfig &record)=0`
- [L22](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.h#L22): `virtual bool write(uint8_t slot,const StoredMotionConfig &record)=0`
- [L30](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.h#L30): `explicit MotionConfigStore(IMotionConfigStorage &storage):_storage(storage)`
- [L31](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.h#L31): `bool load(MotionSettings &settings,uint32_t &revision)`
- [L32](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.h#L32): `bool save(const MotionSettings &settings,uint32_t revision)`
- [L34](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.h#L34): `static uint32_t checksum(const StoredMotionConfig &record)`
- [L35](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfigStore.h#L35): `static bool valid(const StoredMotionConfig &record)`

### PanoduinoControlGitRepo/PanoDuinoControl/MotionConfiguration.h

- [L104](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfiguration.h#L104): `inline AxisMotionConfiguration panMotionConfiguration()`
- [L110](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionConfiguration.h#L110): `inline AxisMotionConfiguration tiltMotionConfiguration()`

### PanoduinoControlGitRepo/PanoDuinoControl/MotionController.cpp

- [L6](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L6): `static double bounded(double value, double minimum, double maximum)`
- [L8](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L8): `static int8_t polarity(double value)`
- [L9](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L9): `static double bezier(const double *points, unsigned int count, double time)`
- [L18](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L18): `uint32_t MotionController::plannedMoveMaximumDurationMs() const`
- [L51](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L51): `MotionProfile::MotionProfile()`
- [L53](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L53): `bool MotionProfile::plan(double position, double velocity, double acceleration, double target, double minimum, double maximum, uint16_t speedPermille, const AxisMotionConfiguration &configuration)`
- [L104](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L104): `void MotionProfile::evaluate(double seconds, double &position, double &velocity, double &acceleration) const`
- [L117](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L117): `MotionController::MotionController()`
- [L124](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L124): `MotionController::MotionController(const AxisMotionConfiguration &panConfiguration, const AxisMotionConfiguration &tiltConfiguration)`
- [L131](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L131): `void MotionController::initializeAxes(const AxisMotionConfiguration &panConfiguration, const AxisMotionConfiguration &tiltConfiguration)`
- [L155](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L155): `void MotionController::configureLimits(double panMinimum, double panMaximum, double tiltMinimum, double tiltMaximum)`
- [L161](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L161): `bool MotionController::configure(const MotionSettings &settings)`
- [L169](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L169): `bool MotionController::command(double panTarget, double tiltTarget, uint16_t panSpeed, uint16_t tiltSpeed)`
- [L234](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L234): `bool MotionController::commandManual(double pan,double tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L264](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L264): `bool MotionController::commandJog(bool tiltAxis,int direction,uint16_t tempo)`
- [L287](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L287): `bool MotionController::commandPositioning(double pan,double tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L294](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L294): `double MotionController::targetVelocity(bool tilt,uint16_t tempo) const`
- [L300](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L300): `bool MotionController::commandColumnReturn(double pan,double tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L311](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L311): `bool MotionController::commandJogVelocity(bool tiltAxis,int direction,double velocityDps)`
- [L319](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L319): `void MotionController::observe(Axis &axis, double position, double dt, bool resetVelocity, uint32_t nowMs)`
- [L334](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L334): `double MotionController::span(const Axis &axis)`
- [L343](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L343): `double MotionController::quietVelocity(const Axis &axis, uint32_t nowMs)`
- [L349](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L349): `double MotionController::meanPosition(const Axis &axis)`
- [L352](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L352): `double MotionController::recentPosition(const Axis &axis, uint8_t maximumSamples)`
- [L362](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L362): `double MotionController::windowVelocity(const Axis &axis, uint32_t nowMs, uint8_t maximumSamples)`
- [L384](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L384): `bool MotionController::stagedTravel(const Axis &axis)`
- [L387](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L387): `double MotionController::jogVelocityGain(const Axis &axis,MotionParameterId low,MotionParameterId high) const`
- [L393](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L393): `double MotionController::controlTravel(Axis &axis, uint32_t nowMs, double dt)`
- [L567](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L567): `double MotionController::controlJogPan(Axis &axis,uint32_t nowMs,double dt)`
- [L681](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L681): `void MotionController::beginFine(Axis &axis,uint32_t nowMs,double velocity)`
- [L692](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L692): `double MotionController::controlFine(Axis &axis,uint32_t nowMs,double dt)`
- [L730](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L730): `void MotionController::control(Axis &axis, uint32_t nowMs, double dt)`
- [L935](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L935): `void MotionController::tick(uint32_t nowMs, double panPosition, double tiltPosition, bool sensorsValid, bool enabled)`
- [L965](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L965): `MotionTraceSnapshot MotionController::traceSnapshot(bool tiltAxis) const`
- [L976](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L976): `void MotionController::disable()`
- [L991](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L991): `void MotionController::targetMissed(Axis &axis)`
- [L999](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L999): `void MotionController::fail(const char *reason)`
- [L1002](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L1002): `void MotionController::resetFeedback()`
- [L1012](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L1012): `bool MotionController::quietSince(uint32_t &sinceMs) const`
- [L1021](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L1021): `bool MotionController::passiveCaptureStable(uint32_t now,double pan,double tilt) const`
- [L1032](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L1032): `bool MotionController::reached() const`
- [L1035](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L1035): `uint8_t MotionController::confirmedSamples() const`
- [L1045](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.cpp#L1045): `AxisMotionStatus MotionController::status(bool tiltAxis) const`

### PanoduinoControlGitRepo/PanoDuinoControl/MotionController.h

- [L13](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L13): `MotionProfile()`
- [L14](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L14): `bool plan(double position, double velocity, double acceleration, double target, double minimum, double maximum, uint16_t speedPermille, const AxisMotionConfiguration &configuration)`
- [L17](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L17): `void evaluate(double seconds, double &position, double &velocity, double &acceleration) const`
- [L18](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L18): `double duration() const`
- [L51](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L51): `MotionController()`
- [L52](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L52): `MotionController(const AxisMotionConfiguration &panConfiguration, const AxisMotionConfiguration &tiltConfiguration)`
- [L53](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L53): `void configureLimits(double panMinimum, double panMaximum, double tiltMinimum, double tiltMaximum)`
- [L54](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L54): `bool configure(const MotionSettings &settings)`
- [L55](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L55): `bool configurationAllowed() const`
- [L57](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L57): `const MotionSettings &settings() const`
- [L58](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L58): `bool command(double panTarget, double tiltTarget, uint16_t panSpeed, uint16_t tiltSpeed)`
- [L59](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L59): `bool commandManual(double pan,double tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L60](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L60): `bool commandPositioning(double pan,double tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L61](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L61): `double targetVelocity(bool tilt,uint16_t tempo) const`
- [L62](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L62): `bool commandColumnReturn(double pan,double tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L63](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L63): `bool commandJog(bool tiltAxis, int direction, uint16_t tempo)`
- [L64](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L64): `bool commandJogVelocity(bool tiltAxis, int direction, double velocityDps)`
- [L65](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L65): `bool jogLimitReached() const`
- [L66](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L66): `void tick(uint32_t nowMs, double panPosition, double tiltPosition, bool sensorsValid, bool enabled)`
- [L67](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L67): `void disable()`
- [L68](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L68): `void resetFeedback()`
- [L69](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L69): `const char *fault() const`
- [L70](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L70): `bool reached() const`
- [L71](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L71): `bool quietSince(uint32_t &sinceMs) const`
- [L72](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L72): `uint32_t sampleSequence() const`
- [L73](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L73): `bool feedbackFresh(uint32_t now) const`
- [L74](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L74): `bool feedbackResting(uint32_t now) const`
- [L78](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L78): `uint8_t confirmedSamples() const`
- [L79](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L79): `bool passiveCaptureStable(uint32_t now,double pan,double tilt) const`
- [L80](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L80): `AxisMotionStatus status(bool tiltAxis) const`
- [L81](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L81): `uint32_t plannedMoveMaximumDurationMs() const`
- [L83](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L83): `MotionTraceSnapshot traceSnapshot(bool tiltAxis) const`
- [L106](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L106): `void initializeAxes(const AxisMotionConfiguration &panConfiguration,const AxisMotionConfiguration &tiltConfiguration)`
- [L107](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L107): `void observe(Axis &axis, double position, double dt, bool resetVelocity, uint32_t nowMs)`
- [L108](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L108): `void control(Axis &axis, uint32_t nowMs, double dt)`
- [L109](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L109): `double controlTravel(Axis &axis, uint32_t nowMs, double dt)`
- [L110](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L110): `double controlJogPan(Axis &axis, uint32_t nowMs, double dt)`
- [L111](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L111): `double jogVelocityGain(const Axis &axis, MotionParameterId low, MotionParameterId high) const`
- [L112](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L112): `void beginFine(Axis &axis,uint32_t nowMs,double velocity)`
- [L113](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L113): `double controlFine(Axis &axis,uint32_t nowMs,double dt)`
- [L114](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L114): `static bool stagedTravel(const Axis &axis)`
- [L115](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L115): `void fail(const char *reason)`
- [L116](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L116): `void targetMissed(Axis &axis)`
- [L117](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L117): `static double span(const Axis &axis)`
- [L118](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L118): `static double meanPosition(const Axis &axis)`
- [L119](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L119): `static double recentPosition(const Axis &axis, uint8_t maximumSamples)`
- [L120](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L120): `static double quietVelocity(const Axis &axis, uint32_t nowMs)`
- [L121](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionController.h#L121): `static double windowVelocity(const Axis &axis, uint32_t nowMs, uint8_t maximumSamples)`

### PanoduinoControlGitRepo/PanoDuinoControl/MotionSettings.cpp

- [L3](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.cpp#L3): `const char *MotionSettings::validate() const`
- [L45](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.cpp#L45): `AxisMotionConfiguration MotionSettings::axis(bool tilt) const`
- [L59](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.cpp#L59): `void MotionSettings::setAxis(bool tilt,const AxisMotionConfiguration &c)`

### PanoduinoControlGitRepo/PanoDuinoControl/MotionSettings.h

- [L22](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.h#L22): `inline const MotionParameterDescriptor &motionParameter(unsigned int index)`
- [L38](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.h#L38): `MotionSettings()`
- [L40](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.h#L40): `double number(MotionParameterId id) const`
- [L42](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.h#L42): `uint32_t milliseconds(MotionParameterId id) const`
- [L43](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.h#L43): `bool set(unsigned int id,int32_t value)`
- [L50](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.h#L50): `const char *validate() const`
- [L51](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.h#L51): `AxisMotionConfiguration axis(bool tilt) const`
- [L52](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/MotionSettings.h#L52): `void setAxis(bool tilt,const AxisMotionConfiguration &configuration)`

### PanoduinoControlGitRepo/PanoDuinoControl/PanoDuinoControl.ino

- [L31](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L31): `extern FspTimer* __get_timer_for_channel(int channel)`
- [L53](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L53): `static bool encoderReadResult(uint8_t address, uint8_t error)`
- [L75](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L75): `SerialTransport wirelessTransport(Wireless_Serial, true)`
- [L77](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L77): `SerialTransport wirelessTransport(Wireless_Serial)`
- [L84](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L84): `bool read(uint8_t slot,StoredMotionConfig &record)`
- [L95](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L95): `bool write(uint8_t slot,const StoredMotionConfig &record)`
- [L116](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L116): `PanoHardwareAdapter()`
- [L117](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L117): `uint16_t columnReturnTiltTempo(uint16_t rowTempo) const`
- [L122](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L122): `uint32_t incrementalMoveMaximumDurationMs() const`
- [L124](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L124): `uint32_t positioningMoveMaximumDurationMs() const`
- [L126](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L126): `bool supportsManualJog() const`
- [L127](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L127): `bool startManualJog(bool tilt,int direction,uint16_t tempo)`
- [L129](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L129): `uint16_t manualJogMaximumVelocityMdeg() const`
- [L130](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L130): `uint16_t manualJogMaximumPanVelocityMdeg() const`
- [L131](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L131): `bool startManualJogVelocity(bool tilt,int direction,uint16_t velocityMdeg)`
- [L133](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L133): `bool manualJogLimitReached() const`
- [L134](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L134): `uint32_t manualJogLeaseMs() const`
- [L135](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L135): `bool setTargetMdeg(int32_t pan, int32_t tilt, uint16_t panSpeed, uint16_t tiltSpeed, bool)`
- [L143](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L143): `bool setManualTargetMdeg(int32_t pan,int32_t tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L149](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L149): `bool targetsReached()`
- [L150](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L150): `bool setColumnReturnTargetMdeg(int32_t pan,int32_t tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L156](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L156): `bool setPositioningTargetMdeg(int32_t pan,int32_t tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L162](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L162): `bool exposurePositionStable(int32_t pan,int32_t tilt)`
- [L164](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L164): `uint32_t captureStabilityWaitMs() const`
- [L165](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L165): `bool stoppedPositionResting() const`
- [L167](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L167): `bool stoppedPositionStable(int32_t pan,int32_t tilt) const`
- [L174](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L174): `bool targetQuietSince(uint32_t &since) const`
- [L175](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L175): `const char *motionFault() const`
- [L176](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L176): `void configureMotionLimits(int32_t panMin, int32_t panMax, int32_t tiltMin, int32_t tiltMax)`
- [L183](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L183): `void controlSampleCompleted(uint32_t now, bool enabled)`
- [L193](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L193): `int drive(bool tilt) const`
- [L200](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L200): `void setCameraActive(bool active)`
- [L206](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L206): `void stopAllMotion()`
- [L214](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L214): `bool startBenchDrive(bool tilt,int pwm,uint32_t duration,int32_t travel,uint32_t now)`
- [L220](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L220): `bool getBenchStatus(BenchDriveStatus &value) const`
- [L221](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L221): `bool startBenchCruise(bool tilt,int pwm,uint32_t duration,int32_t travel,uint32_t now)`
- [L227](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L227): `bool getBenchSample(uint16_t index,BenchDriveSample &value) const`
- [L228](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L228): `bool getBenchOutput(BenchOutputStatus &value) const`
- [L240](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L240): `int32_t actualPanMdeg() const`
- [L241](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L241): `int32_t actualTiltMdeg() const`
- [L242](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L242): `bool setTiltZeroOffsetMdeg(int32_t offset)`
- [L250](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L250): `void getMotionDiagnostics(MotionDiagnosticsV2 &diagnostics) const`
- [L263](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L263): `uint8_t captureTargetErrorFlags(int32_t pan,int32_t tilt) const`
- [L268](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L268): `bool getMotionTelemetry(MotionTelemetryV2 &telemetry) const`
- [L282](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L282): `void loadMotionConfiguration()`
- [L286](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L286): `MotionConfigStore store(boardMotionConfigStorage)`
- [L290](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L290): `bool getMotionConfiguration(MotionSettings &settings,uint32_t &revision) const`
- [L298](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L298): `const char *applyMotionConfiguration(const MotionSettings &settings,uint32_t revision)`
- [L305](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L305): `MotionConfigStore store(boardMotionConfigStorage)`
- [L314](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L314): `void writeMotionTrace(uint32_t nowMs, int appliedPanPwm, int appliedTiltPwm, bool enabled, bool sensorsValid) const`
- [L356](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L356): `void serviceBufferedPanTrace(bool enabled) const`
- [L401](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L401): `V2Controller v2Controller(wirelessTransport, v2Hardware)`
- [L402](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L402): `AMS_AS5048B mysensor_x(PanSensorADDRESS), mysensor_y(TiltSensorADDRESS)`
- [L404](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L404): `IPanoramaHardware &testHardwareAdapter()`
- [L405](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L405): `void testHardwareSample(uint32_t now,bool enabled)`
- [L408](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L408): `void setup()`
- [L435](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L435): `bool readEncoderDegrees(uint8_t address, double &degrees)`
- [L461](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L461): `bool readAxis(uint8_t address, double &previous, double &offset, double &absolute)`
- [L472](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L472): `void writeMotor(int pwmPin, int positivePin, int negativePin, int signedPwm, int &previousDrive)`
- [L488](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L488): `static bool recoverEncoderBus(uint32_t now)`
- [L526](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L526): `static void sampleEncoders(uint32_t now)`
- [L560](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoDuinoControl.ino#L560): `void loop()`

### PanoduinoControlGitRepo/PanoDuinoControl/PanoramaExecutor.cpp

- [L4](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L4): `PanoramaExecutor::PanoramaExecutor(IPanoramaHardware &hardware, SafetyController &safety, IExecutorEvents &events)`
- [L16](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L16): `const char *PanoramaExecutor::loadPlan(const PanoramaPlanV2 &plan)`
- [L41](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L41): `const char *PanoramaExecutor::start(uint32_t runId, uint32_t nowMs)`
- [L50](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L50): `const char *PanoramaExecutor::pause(uint32_t runId)`
- [L64](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L64): `const char *PanoramaExecutor::resume(uint32_t runId)`
- [L74](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L74): `const char *PanoramaExecutor::rewindColumns(uint32_t runId, uint16_t columns)`
- [L93](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L93): `const char *PanoramaExecutor::forwardColumns(uint32_t runId, uint16_t columns)`
- [L114](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L114): `const char *PanoramaExecutor::retryColumn(uint32_t runId,uint16_t previous)`
- [L128](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L128): `void PanoramaExecutor::applyForwardColumns(uint16_t columns)`
- [L143](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L143): `void PanoramaExecutor::applyRewindColumns(uint16_t columns)`
- [L160](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L160): `const char *PanoramaExecutor::updateTiming(uint32_t runId, uint32_t settleMs, uint32_t triggerMs, uint32_t exposureMs)`
- [L179](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L179): `void PanoramaExecutor::applyTiming(uint32_t settleMs, uint32_t triggerMs, uint32_t exposureMs)`
- [L188](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L188): `const char *PanoramaExecutor::stop(uint32_t runId, const char *reason)`
- [L205](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L205): `void PanoramaExecutor::emergencyStop(const char *reason)`
- [L218](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L218): `void PanoramaExecutor::fault(const char *reason)`
- [L231](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L231): `bool PanoramaExecutor::stopMissedTarget(const char *reason,uint32_t nowMs)`
- [L244](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L244): `bool PanoramaExecutor::pauseRecoverable(const char *reason)`
- [L271](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L271): `void PanoramaExecutor::beginPassiveSettling(uint32_t nowMs)`
- [L278](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L278): `void PanoramaExecutor::clearRun()`
- [L295](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L295): `void PanoramaExecutor::tick(uint32_t nowMs)`
- [L468](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L468): `void PanoramaExecutor::transition(RunStateV2 state, RunPhaseV2 phase)`
- [L478](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L478): `bool PanoramaExecutor::selectCurrentTarget()`
- [L483](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.cpp#L483): `bool PanoramaExecutor::elapsed(uint32_t nowMs, uint32_t durationMs) const`

### PanoduinoControlGitRepo/PanoDuinoControl/PanoramaExecutor.h

- [L37](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L37): `virtual ~IPanoramaHardware()`
- [L38](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L38): `virtual bool setTargetMdeg(int32_t panMdeg, int32_t tiltMdeg, uint16_t panSpeedPermille, uint16_t tiltSpeedPermille, bool requireMeasuredProgress) = 0`
- [L39](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L39): `virtual bool setManualTargetMdeg(int32_t pan,int32_t tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L41](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L41): `virtual bool setPositioningTargetMdeg(int32_t pan,int32_t tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L43](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L43): `virtual bool setColumnReturnTargetMdeg(int32_t pan,int32_t tilt,uint16_t panSpeed,uint16_t tiltSpeed)`
- [L45](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L45): `virtual uint16_t columnReturnTiltTempo(uint16_t rowTempo) const`
- [L46](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L46): `virtual uint32_t incrementalMoveMaximumDurationMs() const`
- [L47](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L47): `virtual uint32_t positioningMoveMaximumDurationMs() const`
- [L48](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L48): `virtual bool supportsManualJog() const`
- [L49](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L49): `virtual bool startManualJog(bool, int, uint16_t)`
- [L50](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L50): `virtual uint16_t manualJogMaximumVelocityMdeg() const`
- [L51](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L51): `virtual uint16_t manualJogMaximumPanVelocityMdeg() const`
- [L52](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L52): `virtual bool startManualJogVelocity(bool, int, uint16_t)`
- [L53](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L53): `virtual bool manualJogLimitReached() const`
- [L54](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L54): `virtual uint32_t manualJogLeaseMs() const`
- [L55](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L55): `virtual bool targetsReached() = 0`
- [L56](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L56): `virtual bool exposurePositionStable(int32_t pan, int32_t tilt)`
- [L57](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L57): `virtual uint32_t captureStabilityWaitMs() const`
- [L59](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L59): `virtual bool stoppedPositionResting() const`
- [L60](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L60): `virtual bool stoppedPositionStable(int32_t, int32_t) const`
- [L61](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L61): `virtual void setCameraActive(bool active) = 0`
- [L62](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L62): `virtual void stopAllMotion() = 0`
- [L63](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L63): `virtual int32_t actualPanMdeg() const = 0`
- [L64](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L64): `virtual int32_t actualTiltMdeg() const = 0`
- [L65](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L65): `virtual bool setTiltZeroOffsetMdeg(int32_t)`
- [L66](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L66): `virtual void getMotionDiagnostics(MotionDiagnosticsV2 &diagnostics) const = 0`
- [L68](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L68): `virtual uint8_t captureTargetErrorFlags(int32_t, int32_t) const`
- [L69](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L69): `virtual void configureMotionLimits(int32_t, int32_t, int32_t, int32_t)`
- [L70](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L70): `virtual bool targetQuietSince(uint32_t &) const`
- [L71](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L71): `virtual const char *motionFault() const`
- [L72](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L72): `virtual bool getMotionTelemetry(MotionTelemetryV2 &) const`
- [L73](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L73): `virtual bool getMotionConfiguration(MotionSettings &,uint32_t &) const`
- [L74](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L74): `virtual void loadMotionConfiguration()`
- [L75](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L75): `virtual const char *applyMotionConfiguration(const MotionSettings &,uint32_t)`
- [L77](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L77): `virtual bool startBenchDrive(bool, int, uint32_t, int32_t, uint32_t)`
- [L78](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L78): `virtual bool startBenchCruise(bool, int, uint32_t, int32_t, uint32_t)`
- [L79](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L79): `virtual bool getBenchStatus(BenchDriveStatus &) const`
- [L80](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L80): `virtual bool getBenchSample(uint16_t, BenchDriveSample &) const`
- [L81](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L81): `virtual bool getBenchOutput(BenchOutputStatus &) const`
- [L87](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L87): `virtual ~IExecutorEvents()`
- [L88](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L88): `virtual void onStateChanged(RunStateV2 state) = 0`
- [L89](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L89): `virtual void onPhaseChanged(RunPhaseV2 previousPhase, RunPhaseV2 phase) = 0`
- [L90](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L90): `virtual void onAcquisitionRetry(uint32_t, const char *)`
- [L91](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L91): `virtual void onImageCompleted(const TargetPositionV2 &target, int32_t actualPanMdeg, int32_t actualTiltMdeg, uint32_t captureSequence, uint16_t planRevision) = 0`
- [L113](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L113): `PanoramaExecutor(IPanoramaHardware &hardware, SafetyController &safety, IExecutorEvents &events)`
- [L114](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L114): `const char *loadPlan(const PanoramaPlanV2 &plan)`
- [L115](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L115): `const char *start(uint32_t runId, uint32_t nowMs)`
- [L116](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L116): `const char *pause(uint32_t runId)`
- [L117](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L117): `const char *resume(uint32_t runId)`
- [L118](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L118): `const char *rewindColumns(uint32_t runId, uint16_t columns)`
- [L119](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L119): `const char *forwardColumns(uint32_t runId, uint16_t columns)`
- [L120](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L120): `const char *updateTiming(uint32_t runId, uint32_t settleMs, uint32_t triggerMs, uint32_t exposureMs)`
- [L121](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L121): `const char *stop(uint32_t runId, const char *reason)`
- [L122](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L122): `void emergencyStop(const char *reason)`
- [L123](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L123): `void fault(const char *reason)`
- [L124](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L124): `bool stopMissedTarget(const char *reason,uint32_t nowMs)`
- [L125](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L125): `bool pauseRecoverable(const char *reason)`
- [L126](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L126): `const char *retryColumn(uint32_t runId,uint16_t previous)`
- [L127](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L127): `void clearRun()`
- [L128](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L128): `void tick(uint32_t nowMs)`
- [L130](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L130): `RunStateV2 state() const`
- [L131](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L131): `RunPhaseV2 phase() const`
- [L132](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L132): `const PanoramaPlanV2 &plan() const`
- [L133](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L133): `const TargetPositionV2 &currentTarget() const`
- [L134](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L134): `uint32_t completedImages() const`
- [L135](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L135): `uint32_t nextImageIndex() const`
- [L136](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L136): `uint16_t planRevision() const`
- [L137](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L137): `uint8_t segmentCount() const`
- [L138](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L138): `const RunSegmentV2 &segment(uint8_t index) const`
- [L139](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L139): `bool pauseRequested() const`
- [L140](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L140): `uint16_t pendingRewindColumns() const`
- [L141](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L141): `uint16_t pendingForwardColumns() const`
- [L142](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L142): `bool lastNavigationForward() const`
- [L143](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L143): `uint16_t timingRevision() const`
- [L144](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L144): `const char *lastError() const`
- [L145](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L145): `uint32_t phaseStartedMs() const`
- [L146](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L146): `uint32_t movementStartedMs() const`
- [L147](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L147): `uint32_t activeMoveMaximumDurationMs() const`
- [L152](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L152): `void transition(RunStateV2 state, RunPhaseV2 phase)`
- [L153](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L153): `bool selectCurrentTarget()`
- [L154](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L154): `void applyRewindColumns(uint16_t columns)`
- [L155](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L155): `void applyForwardColumns(uint16_t columns)`
- [L156](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L156): `void applyTiming(uint32_t settleMs, uint32_t triggerMs, uint32_t exposureMs)`
- [L157](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L157): `bool elapsed(uint32_t nowMs, uint32_t durationMs) const`
- [L158](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaExecutor.h#L158): `void beginPassiveSettling(uint32_t nowMs)`

### PanoduinoControlGitRepo/PanoDuinoControl/PanoramaPlanV2.cpp

- [L4](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.cpp#L4): `PanoramaPlanV2::PanoramaPlanV2()`
- [L12](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.cpp#L12): `uint32_t PanoramaPlanV2::totalImages() const`
- [L29](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.cpp#L29): `static int64_t roundDivide(int64_t value, int64_t divisor)`
- [L34](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.cpp#L34): `uint32_t PanoramaPlanV2::shotsInBand(uint16_t band) const`
- [L54](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.cpp#L54): `bool PanoramaPlanV2::targetFor(uint32_t imageIndex, TargetPositionV2 &target) const`
- [L93](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.cpp#L93): `bool PanoramaPlanV2::timingValid(uint32_t settleMs, uint32_t triggerMs, uint32_t exposureMs)`
- [L98](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.cpp#L98): `const char *PanoramaPlanV2::validate(const SafetyController &safety) const`

### PanoduinoControlGitRepo/PanoDuinoControl/PanoramaPlanV2.h

- [L38](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.h#L38): `PanoramaPlanV2()`
- [L39](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.h#L39): `uint32_t totalImages() const`
- [L40](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.h#L40): `bool targetFor(uint32_t imageIndex, TargetPositionV2 &target) const`
- [L41](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.h#L41): `uint32_t shotsInBand(uint16_t band) const`
- [L42](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.h#L42): `static bool timingValid(uint32_t settleMs, uint32_t triggerMs, uint32_t exposureMs)`
- [L43](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanoramaPlanV2.h#L43): `const char *validate(const SafetyController &safety) const`

### PanoduinoControlGitRepo/PanoDuinoControl/PanStopModel.h

- [L11](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/PanStopModel.h#L11): `static bool estimate(const double *time,const double *position,unsigned int count,double &nowPosition,double &nowVelocity)`

### PanoduinoControlGitRepo/PanoDuinoControl/ProtocolMessages.cpp

- [L3](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolMessages.cpp#L3): `const char *runStateName(RunStateV2 state)`
- [L19](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolMessages.cpp#L19): `const char *runPhaseName(RunPhaseV2 phase)`

### PanoduinoControlGitRepo/PanoDuinoControl/ProtocolMessages.h

- [L33](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolMessages.h#L33): `const char *runStateName(RunStateV2 state)`
- [L34](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolMessages.h#L34): `const char *runPhaseName(RunPhaseV2 phase)`

### PanoduinoControlGitRepo/PanoDuinoControl/ProtocolV2.cpp

- [L14](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L14): `ProtocolV2::ProtocolV2(Stream &stream, IV2FrameHandler &handler)`
- [L25](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L25): `void ProtocolV2::tick(uint32_t nowMs, bool preserveLegacyBytes)`
- [L81](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L81): `void ProtocolV2::startReceiver(uint32_t nowMs)`
- [L90](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L90): `void ProtocolV2::resetReceiver()`
- [L97](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L97): `void ProtocolV2::parseCompleteFrame(uint32_t nowMs)`
- [L193](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L193): `void ProtocolV2::sendAck(uint16_t requestSequence, const char *resultCode)`
- [L211](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L211): `void ProtocolV2::sendNack(uint16_t requestSequence, const char *errorCode)`
- [L229](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L229): `bool ProtocolV2::sendFrame(const char *messageType, const char *fields)`
- [L236](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L236): `void ProtocolV2::beginFrame(const char *messageType)`
- [L247](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L247): `void ProtocolV2::beginFrame(const __FlashStringHelper *messageType)`
- [L258](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L258): `void ProtocolV2::addField(const char *value)`
- [L266](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L266): `void ProtocolV2::addField(const __FlashStringHelper *value)`
- [L274](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L274): `void ProtocolV2::addField(uint32_t value)`
- [L281](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L281): `void ProtocolV2::addSignedField(int32_t value)`
- [L294](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L294): `bool ProtocolV2::endFrame()`
- [L312](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L312): `void ProtocolV2::writeBodyChar(char value)`
- [L322](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L322): `void ProtocolV2::writeBodyText(const char *value)`
- [L329](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L329): `void ProtocolV2::writeBodyFlash(const __FlashStringHelper *value)`
- [L337](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L337): `void ProtocolV2::writeBodyUnsigned(uint32_t value)`
- [L349](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L349): `uint16_t ProtocolV2::crc16(const char *data, size_t length)`
- [L363](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L363): `bool ProtocolV2::parseUnsigned(const char *value, uint32_t maximum, uint32_t &result)`
- [L379](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L379): `void ProtocolV2::sendAck(uint16_t requestSequence, const __FlashStringHelper *resultCode)`
- [L397](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.cpp#L397): `void ProtocolV2::sendNack(uint16_t requestSequence, const __FlashStringHelper *errorCode)`

### PanoduinoControlGitRepo/PanoDuinoControl/ProtocolV2.h

- [L30](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L30): `virtual ~IV2FrameHandler()`
- [L31](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L31): `virtual void handleV2Frame(const V2Frame &frame, ProtocolV2 &protocol, uint32_t nowMs) = 0`
- [L37](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L37): `ProtocolV2(Stream &stream, IV2FrameHandler &handler)`
- [L38](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L38): `void tick(uint32_t nowMs, bool preserveLegacyBytes = true)`
- [L39](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L39): `bool isReceiving() const`
- [L40](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L40): `int transmitSpace()`
- [L42](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L42): `void sendAck(uint16_t requestSequence, const char *resultCode)`
- [L43](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L43): `void sendNack(uint16_t requestSequence, const char *errorCode)`
- [L44](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L44): `bool sendFrame(const char *messageType, const char *fields)`
- [L45](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L45): `void beginFrame(const char *messageType)`
- [L46](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L46): `void addField(const char *value)`
- [L47](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L47): `void addField(uint32_t value)`
- [L48](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L48): `void addSignedField(int32_t value)`
- [L49](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L49): `bool endFrame()`
- [L51](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L51): `void sendAck(uint16_t requestSequence, const __FlashStringHelper *resultCode)`
- [L52](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L52): `void sendNack(uint16_t requestSequence, const __FlashStringHelper *errorCode)`
- [L53](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L53): `void beginFrame(const __FlashStringHelper *messageType)`
- [L54](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L54): `void addField(const __FlashStringHelper *value)`
- [L57](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L57): `static uint16_t crc16(const char *data, size_t length)`
- [L60](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L60): `void resetReceiver()`
- [L61](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L61): `void startReceiver(uint32_t nowMs)`
- [L62](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L62): `void parseCompleteFrame(uint32_t nowMs)`
- [L63](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L63): `static bool parseUnsigned(const char *value, uint32_t maximum, uint32_t &result)`
- [L64](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L64): `void writeBodyChar(char value)`
- [L65](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L65): `void writeBodyText(const char *value)`
- [L67](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L67): `void writeBodyFlash(const __FlashStringHelper *value)`
- [L69](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/ProtocolV2.h#L69): `void writeBodyUnsigned(uint32_t value)`

### PanoduinoControlGitRepo/PanoDuinoControl/SafetyController.cpp

- [L17](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L17): `static uint32_t tiltZeroCheck(int32_t offset)`
- [L18](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L18): `static uint32_t tiltStoreCheck(int32_t minimum, int32_t maximum)`
- [L28](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L28): `SafetyController::SafetyController()`
- [L34](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L34): `void SafetyController::beginBootSession()`
- [L60](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L60): `void SafetyController::loadTiltLimits()`
- [L87](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L87): `bool SafetyController::setTiltZeroOffsetMdeg(int32_t offsetMdeg)`
- [L104](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L104): `bool SafetyController::setTiltLimits(int32_t minimumMdeg, int32_t maximumMdeg)`
- [L131](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L131): `SafetyResult SafetyController::validateTarget(int32_t panMdeg, int32_t tiltMdeg) const`
- [L139](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L139): `SafetyResult SafetyController::observeTilt(int32_t tiltMdeg)`
- [L156](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L156): `static bool manualAxisTargetAllowed(int32_t actual, int32_t target, int32_t minimum, int32_t maximum)`
- [L163](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L163): `SafetyResult SafetyController::validateManualTarget(int32_t actualPan, int32_t actualTilt, int32_t targetPan, int32_t targetTilt) const`
- [L180](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L180): `int SafetyController::limitDrive(bool tiltAxis, int32_t positionMdeg, int signedPwm) const`
- [L190](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L190): `bool SafetyController::limitsConfirmed() const`
- [L195](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L195): `bool SafetyController::motionEnabled() const`
- [L200](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L200): `void SafetyController::enableMotion()`
- [L206](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L206): `void SafetyController::stopMotion()`
- [L211](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.cpp#L211): `const char *SafetyController::lastFault() const`

### PanoduinoControlGitRepo/PanoDuinoControl/SafetyController.h

- [L18](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L18): `SafetyController()`
- [L19](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L19): `SafetyResult validateTarget(int32_t panMdeg, int32_t tiltMdeg) const`
- [L20](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L20): `SafetyResult validateManualTarget(int32_t actualPan, int32_t actualTilt, int32_t targetPan, int32_t targetTilt) const`
- [L21](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L21): `int limitDrive(bool tiltAxis, int32_t positionMdeg, int signedPwm) const`
- [L22](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L22): `SafetyResult observeTilt(int32_t tiltMdeg)`
- [L23](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L23): `bool limitsConfirmed() const`
- [L24](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L24): `bool motionEnabled() const`
- [L25](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L25): `void enableMotion()`
- [L26](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L26): `void stopMotion()`
- [L27](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L27): `const char *lastFault() const`
- [L28](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L28): `void loadTiltLimits()`
- [L29](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L29): `bool setTiltLimits(int32_t minimumMdeg, int32_t maximumMdeg)`
- [L30](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L30): `bool setTiltZeroOffsetMdeg(int32_t offsetMdeg)`
- [L31](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L31): `int32_t tiltZeroOffsetMdeg() const`
- [L32](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L32): `void beginBootSession()`
- [L33](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L33): `uint32_t bootId() const`
- [L35](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L35): `int32_t panMinimumMdeg() const`
- [L36](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L36): `int32_t panMaximumMdeg() const`
- [L37](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L37): `int32_t tiltMinimumMdeg() const`
- [L38](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SafetyController.h#L38): `int32_t tiltMaximumMdeg() const`

### PanoduinoControlGitRepo/PanoDuinoControl/SerialTransport.h

- [L12](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h#L12): `explicit SerialTransport(Stream &serial, bool synchronousUart = false)`
- [L14](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h#L14): `int available()`
- [L15](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h#L15): `int peek()`
- [L16](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h#L16): `int read()`
- [L17](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h#L17): `int availableForWrite()`
- [L18](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h#L18): `size_t write(uint8_t value)`
- [L23](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h#L23): `void flush()`
- [L25](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h#L25): `size_t print(const char *value)`
- [L30](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h#L30): `void service()`
- [L39](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h#L39): `bool takeOverflow()`
- [L41](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/SerialTransport.h#L41): `void discardPending()`

### PanoduinoControlGitRepo/PanoDuinoControl/StackWatermark.h

- [L14](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/StackWatermark.h#L14): `StackWatermark()`
- [L16](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/StackWatermark.h#L16): `void begin()`
- [L44](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/StackWatermark.h#L44): `uint16_t currentFreeBytes() const`
- [L55](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/StackWatermark.h#L55): `uint16_t minimumFreeBytes() const`
- [L76](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/StackWatermark.h#L76): `static uint8_t *heapStart()`
- [L82](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/StackWatermark.h#L82): `static uint8_t *heapEnd()`

### PanoduinoControlGitRepo/PanoDuinoControl/TargetArrivalMonitor.h

- [L12](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/TargetArrivalMonitor.h#L12): `explicit TargetArrivalMonitor(uint8_t requiredStableSamples)`
- [L21](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/TargetArrivalMonitor.h#L21): `void resetForNewTarget(bool requirePanProgress = false, bool requireTiltProgress = false)`
- [L34](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/TargetArrivalMonitor.h#L34): `void stop()`
- [L41](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/TargetArrivalMonitor.h#L41): `void observe(bool panWithinTolerance, bool tiltWithinTolerance, bool panProgress = false, bool tiltProgress = false)`
- [L62](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/TargetArrivalMonitor.h#L62): `bool reached() const`
- [L63](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/TargetArrivalMonitor.h#L63): `bool panWithinTolerance() const`
- [L64](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/TargetArrivalMonitor.h#L64): `bool tiltWithinTolerance() const`
- [L65](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/TargetArrivalMonitor.h#L65): `uint8_t stableSamples() const`
- [L66](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/TargetArrivalMonitor.h#L66): `uint8_t requiredStableSamples() const`
- [L67](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/TargetArrivalMonitor.h#L67): `uint32_t sampleSequence() const`

### PanoduinoControlGitRepo/PanoDuinoControl/V2Controller.cpp

- [L17](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L17): `V2Controller::V2Controller(Stream &stream, IPanoramaHardware &hardware)`
- [L31](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L31): `void V2Controller::begin(uint32_t nowMs)`
- [L51](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L51): `void V2Controller::tick(uint32_t nowMs)`
- [L87](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L87): `SafetyResult V2Controller::validateLegacyTarget(int32_t panMdeg, int32_t tiltMdeg)`
- [L97](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L97): `void V2Controller::observeTilt(int32_t tiltMdeg)`
- [L108](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L108): `void V2Controller::emergencyStop(const char *reason)`
- [L118](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L118): `void V2Controller::fault(const char *reason)`
- [L136](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L136): `void V2Controller::handleV2Frame(const V2Frame &frame, ProtocolV2 &protocol, uint32_t nowMs)`
- [L655](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L655): `void V2Controller::onStateChanged(RunStateV2 state)`
- [L679](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L679): `void V2Controller::onAcquisitionRetry(uint32_t imageIndex,const char *reason)`
- [L688](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L688): `void V2Controller::onPhaseChanged(RunPhaseV2 previousPhase, RunPhaseV2 phase)`
- [L701](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L701): `void V2Controller::onImageCompleted(const TargetPositionV2 &target, int32_t actualPanMdeg, int32_t actualTiltMdeg, uint32_t captureSequence, uint16_t planRevision)`
- [L735](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L735): `bool V2Controller::parsePlan(const V2Frame &frame, PanoramaPlanV2 &plan, const char *&errorCode)`
- [L773](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L773): `void V2Controller::sendCapabilities()`
- [L815](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L815): `void V2Controller::sendDeviceInfo()`
- [L828](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L828): `void V2Controller::sendDiagnostics(uint32_t nowMs)`
- [L875](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L875): `void V2Controller::sendDebugLog(uint16_t afterSequence)`
- [L906](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L906): `void V2Controller::flushDebugLog()`
- [L921](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L921): `void V2Controller::sendDebugRecord(const DebugEventRecordV2 &record)`
- [L947](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L947): `void V2Controller::recordDebug(DebugEventCodeV2 code)`
- [L982](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L982): `void V2Controller::sendPlan()`
- [L1011](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1011): `void V2Controller::sendSegments()`
- [L1016](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1016): `void V2Controller::sendSegment(uint8_t index)`
- [L1028](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1028): `void V2Controller::sendStatus()`
- [L1068](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1068): `void V2Controller::finishManualStatus(ManualResult result)`
- [L1076](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1076): `void V2Controller::sendManualStatus()`
- [L1103](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1103): `void V2Controller::stopTimelapse()`
- [L1110](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1110): `void V2Controller::tickTimelapse(uint32_t nowMs)`
- [L1129](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1129): `void V2Controller::checkHeartbeat(uint32_t nowMs)`
- [L1145](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1145): `void V2Controller::tickManualTrigger(uint32_t nowMs)`
- [L1157](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1157): `void V2Controller::sendBenchStatus()`
- [L1166](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1166): `void V2Controller::tickBenchDrive(uint32_t)`
- [L1178](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1178): `void V2Controller::tickJog(uint32_t nowMs)`
- [L1189](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1189): `void V2Controller::tickManualMove(uint32_t nowMs)`
- [L1213](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1213): `void V2Controller::sendManualImageTriggered(uint32_t nowMs)`
- [L1231](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1231): `void V2Controller::sendManualRecord(const ManualCaptureRecordV2 &record, bool replayed)`
- [L1245](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1245): `void V2Controller::addRunStateField(RunStateV2 state)`
- [L1260](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1260): `void V2Controller::addRunPhaseField(RunPhaseV2 phase)`
- [L1274](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1274): `bool V2Controller::parseUInt32(const char *value, uint32_t &result) const`
- [L1289](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1289): `bool V2Controller::parseUInt16(const char *value, uint16_t &result) const`
- [L1297](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1297): `bool V2Controller::parseInt32(const char *value, int32_t &result) const`
- [L1319](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1319): `bool V2Controller::requireRunId(const V2Frame &frame, uint32_t &runId, ProtocolV2 &protocol)`
- [L1330](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1330): `bool V2Controller::configurationWritable() const`
- [L1337](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1337): `void V2Controller::sendMotionConfigurationInfo()`
- [L1359](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.cpp#L1359): `bool V2Controller::handleMotionConfiguration(const V2Frame &f,ProtocolV2 &p,uint32_t now)`

### PanoduinoControlGitRepo/PanoDuinoControl/V2Controller.h

- [L24](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L24): `V2Controller(Stream &stream, IPanoramaHardware &hardware)`
- [L25](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L25): `void begin(uint32_t nowMs)`
- [L26](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L26): `void tick(uint32_t nowMs)`
- [L27](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L27): `bool isReceiving() const`
- [L28](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L28): `bool sessionActive() const`
- [L29](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L29): `bool motionEnabled() const`
- [L30](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L30): `int limitDrive(bool tiltAxis, int32_t positionMdeg, int signedPwm) const`
- [L31](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L31): `SafetyResult validateLegacyTarget(int32_t panMdeg, int32_t tiltMdeg)`
- [L32](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L32): `void observeTilt(int32_t tiltMdeg)`
- [L33](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L33): `void emergencyStop(const char *reason)`
- [L34](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L34): `void fault(const char *reason)`
- [L36](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L36): `virtual void handleV2Frame(const V2Frame &frame, ProtocolV2 &protocol, uint32_t nowMs)`
- [L37](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L37): `virtual void onStateChanged(RunStateV2 state)`
- [L38](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L38): `virtual void onPhaseChanged(RunPhaseV2 previousPhase, RunPhaseV2 phase)`
- [L39](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L39): `virtual void onAcquisitionRetry(uint32_t imageIndex, const char *reason)`
- [L40](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L40): `virtual void onImageCompleted(const TargetPositionV2 &target, int32_t actualPanMdeg, int32_t actualTiltMdeg, uint32_t captureSequence, uint16_t planRevision)`
- [L47](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L47): `void tickJog(uint32_t nowMs)`
- [L49](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L49): `bool parsePlan(const V2Frame &frame, PanoramaPlanV2 &plan, const char *&errorCode)`
- [L50](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L50): `void sendCapabilities()`
- [L51](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L51): `void sendDeviceInfo()`
- [L52](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L52): `void sendDiagnostics(uint32_t nowMs)`
- [L54](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L54): `void sendDebugLog(uint16_t afterSequence)`
- [L55](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L55): `void sendDebugRecord(const DebugEventRecordV2 &record)`
- [L56](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L56): `void recordDebug(DebugEventCodeV2 code)`
- [L57](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L57): `void flushDebugLog()`
- [L59](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L59): `void recordDebug(DebugEventCodeV2)`
- [L61](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L61): `void sendPlan()`
- [L62](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L62): `void sendSegments()`
- [L63](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L63): `void sendSegment(uint8_t index)`
- [L64](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L64): `void sendManualRecord(const ManualCaptureRecordV2 &record, bool replayed)`
- [L65](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L65): `void addRunStateField(RunStateV2 state)`
- [L66](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L66): `void addRunPhaseField(RunPhaseV2 phase)`
- [L67](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L67): `void sendStatus()`
- [L68](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L68): `void sendManualStatus()`
- [L69](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L69): `void finishManualStatus(ManualResult result)`
- [L70](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L70): `void checkHeartbeat(uint32_t nowMs)`
- [L71](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L71): `void tickManualTrigger(uint32_t nowMs)`
- [L72](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L72): `void tickTimelapse(uint32_t nowMs)`
- [L73](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L73): `void stopTimelapse()`
- [L74](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L74): `void tickManualMove(uint32_t nowMs)`
- [L75](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L75): `void tickBenchDrive(uint32_t nowMs)`
- [L76](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L76): `void sendBenchStatus()`
- [L77](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L77): `void sendManualImageTriggered(uint32_t nowMs)`
- [L78](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L78): `bool parseUInt32(const char *value, uint32_t &result) const`
- [L79](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L79): `bool parseUInt16(const char *value, uint16_t &result) const`
- [L80](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L80): `bool parseInt32(const char *value, int32_t &result) const`
- [L81](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L81): `bool requireRunId(const V2Frame &frame, uint32_t &runId, ProtocolV2 &protocol)`
- [L82](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L82): `bool handleMotionConfiguration(const V2Frame &frame,ProtocolV2 &protocol,uint32_t nowMs)`
- [L83](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L83): `bool configurationWritable() const`
- [L84](https://github.com/TomNaiser/PanoDuinoControl/blob/master/PanoDuinoControl/V2Controller.h#L84): `void sendMotionConfigurationInfo()`

## Android

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java

- [L20](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L20): `void onConnected()`
- [L21](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L21): `void onAwaitingProtocol()`
- [L22](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L22): `void onFrame(P2Codec.Frame frame)`
- [L23](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L23): `void onDisconnected(String reason)`
- [L39](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L39): `public BluetoothSession(Listener listener)`
- [L40](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L40): `BluetoothSession(Listener listener, boolean secure)`
- [L41](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L41): `public void connect(BluetoothDevice device)`
- [L45](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L45): `private void run(BluetoothDevice device)`
- [L97](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L97): `private void readFrames(InputStream input) throws IOException`
- [L110](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L110): `public CompletableFuture<V2Client.Result> send(String type, String... fields)`
- [L115](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L115): `public CompletableFuture<V2Client.Result> emergencyStop(String id)`
- [L120](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L120): `private static CompletableFuture<V2Client.Result> unavailable()`
- [L124](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L124): `public CompletableFuture<V2Client.Result> stopJog(String token)`
- [L129](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/BluetoothSession.java#L129): `public void fail(String reason)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CameraProfile.java

- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CameraProfile.java#L7): `public CameraProfile(String name, double focal, double width, double height, int settle, int exposure)`
- [L14](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CameraProfile.java#L14): `public double fovX()`
- [L15](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CameraProfile.java#L15): `public double fovY()`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureQualityGuard.java

- [L8](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureQualityGuard.java#L8): `Issue(String[] f)`
- [L13](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureQualityGuard.java#L13): `String describe()`
- [L17](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureQualityGuard.java#L17): `Issue accept(String[] f)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStabilitySettings.java

- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStabilitySettings.java#L7): `CaptureStabilitySettings(int span,int pan,int tilt,int wait,long revision)`
- [L13](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStabilitySettings.java#L13): `static CaptureStabilitySettings parse(String[] fields)`
- [L18](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStabilitySettings.java#L18): `static CaptureStabilitySettings fromUi(double span,double pan,double tilt,double seconds,long revision)`
- [L25](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStabilitySettings.java#L25): `String[] fields()`
- [L29](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStabilitySettings.java#L29): `boolean sameValues(CaptureStabilitySettings other)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java

- [L14](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L14): `Run(String key, String title)`
- [L17](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L17): `public CaptureStore(Context context)`
- [L18](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L18): `CaptureStore(Context context, String name)`
- [L20](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L20): `synchronized void onExportChanged(java.util.function.Consumer<String> listener)`
- [L22](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L22): `synchronized List<String> liveRoots()`
- [L29](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L29): `synchronized LivePapywizard.Snapshot liveSnapshot(String selected)`
- [L44](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L44): `private void createWarnings(SQLiteDatabase db)`
- [L53](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L53): `private void createRecovery(SQLiteDatabase db)`
- [L62](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L62): `Checkpoint(String key,String device,long boot,int tiltZero,String error,PanoramaPlan plan,long index)`
- [L67](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L67): `synchronized boolean recordFault(String device,long boot,DeviceState state,String[] fields)`
- [L82](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L82): `synchronized Checkpoint checkpoint(String device,long run)`
- [L89](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L89): `synchronized Checkpoint pendingCheckpoint(String device)`
- [L96](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L96): `synchronized String linkRest(Checkpoint source,PanoramaPlan rest,int startColumn)`
- [L111](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L111): `private List<String> linkedKeys(String selected)`
- [L120](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L120): `private void collectChildren(String key,List<String> result,Set<String> seen)`
- [L129](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L129): `synchronized boolean hasUncertainExposure(String selected)`
- [L135](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L135): `synchronized String exportInterruptions(String selected)`
- [L146](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L146): `private String group(String device, long runId)`
- [L151](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L151): `private String newGroup(String device, long runId)`
- [L157](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L157): `public synchronized boolean accept(String device, DeviceState state, P2Codec.Frame frame) throws IOException`
- [L207](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L207): `private void updateRun(String key, ContentValues values)`
- [L208](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L208): `private long completed(String key)`
- [L211](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L211): `private JSONArray segments(String key) throws JSONException`
- [L214](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L214): `private void reconstruct(String key, DeviceState state) throws JSONException`
- [L251](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L251): `private boolean insert(String run, String identity, String payload, long ordinal, boolean manual, boolean measured)`
- [L262](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L262): `public synchronized List<Run> runs()`
- [L269](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L269): `synchronized List<RunPlotModel.Capture> plotCaptures(String device,long runId)`
- [L274](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L274): `private List<RunPlotModel.Capture> plotCaptures(String key)`
- [L284](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L284): `PlotSnapshot(PanoramaPlan plan,RunPlotModel model,String device)`
- [L286](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L286): `synchronized PlotSnapshot plotSnapshot(String key)`
- [L296](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L296): `public synchronized String export(String key, boolean xml)`
- [L299](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L299): `public synchronized String exportLinked(String key,boolean xml)`
- [L302](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L302): `private String exportKeys(List<String> keys,boolean xml,boolean linked)`
- [L338](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L338): `private static String degrees(String text)`
- [L339](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureStore.java#L339): `private static String tiltDegrees(String text)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureWarning.java

- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureWarning.java#L7): `private CaptureWarning(long run,long capture,int flags)`
- [L8](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureWarning.java#L8): `static CaptureWarning parse(String[] fields)`
- [L15](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CaptureWarning.java#L15): `static String describe(int flags)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CommandEffects.java

- [L5](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/CommandEffects.java#L5): `static boolean readOnly(String type)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceLink.java

- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceLink.java#L7): `CompletableFuture<V2Client.Result> send(String type,String... fields)`
- [L8](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceLink.java#L8): `CompletableFuture<V2Client.Result> emergencyStop(String runId)`
- [L9](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceLink.java#L9): `default CompletableFuture<V2Client.Result> stopJog(String token)`
- [L10](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceLink.java#L10): `void fail(String reason)`
- [L11](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceLink.java#L11): `void close()`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceSession.java

- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceSession.java#L7): `DeviceSession(long previousBootId)`
- [L8](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceSession.java#L8): `boolean accept(long nextBootId)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java

- [L16](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L16): `long currentMoveMaximumMs()`
- [L33](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L33): `public void accept(P2Codec.Frame frame, long now)`
- [L125](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L125): `public boolean fresh(long now)`
- [L126](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L126): `boolean manualStatusFresh(long now)`
- [L127](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L127): `public boolean manual(long now)`
- [L128](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L128): `public boolean navigation(long now)`
- [L132](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L132): `public boolean positionSafe()`
- [L135](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L135): `public void validateTarget(int x, int y)`
- [L140](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L140): `public void validateManualTarget(int x,int y)`
- [L150](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L150): `private static boolean manualAxisAllowed(int actual,int target,int min,int max)`
- [L153](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L153): `static int manualStep(int actual,int candidate,int min,int max)`
- [L157](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L157): `static long unsigned(String text)`
- [L162](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L162): `public static int toDevice(double degrees)`
- [L168](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/DeviceState.java#L168): `public static int toTiltDevice(double degrees)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/EndpointSelection.java

- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/EndpointSelection.java#L7): `void propose(double x,double y,double committedTilt,double minPan,double maxPan,double minTilt,double maxTilt)`
- [L13](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/EndpointSelection.java#L13): `private static double round(double value)`
- [L14](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/EndpointSelection.java#L14): `void clear()`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/FrameReader.java

- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/FrameReader.java#L7): `P2Codec.Frame accept(int value)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/IncidentTracker.java

- [L9](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/IncidentTracker.java#L9): `void observe(String nextKey,String nextDetail)`
- [L13](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/IncidentTracker.java#L13): `boolean acknowledge(long expected)`
- [L17](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/IncidentTracker.java#L17): `void resolved()`
- [L18](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/IncidentTracker.java#L18): `boolean needsAcknowledgement()`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/JogHold.java

- [L8](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/JogHold.java#L8): `boolean begin(int axis,int direction,long now)`
- [L14](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/JogHold.java#L14): `long release()`
- [L18](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/JogHold.java#L18): `void finish(long expected)`
- [L19](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/JogHold.java#L19): `boolean matches(int axis,int direction)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java

- [L12](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L12): `List<String> roots()`
- [L13](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L13): `Snapshot snapshot(String key)`
- [L18](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L18): `Snapshot(String filename,String xml)`
- [L36](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L36): `LivePapywizard(Source source,File directory,Listener listener)`
- [L39](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L39): `LivePapywizard(Source source,File directory,Publisher publisher,Listener listener)`
- [L42](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L42): `String status()`
- [L44](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L44): `synchronized void rebuild()`
- [L55](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L55): `synchronized void request(String key)`
- [L60](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L60): `private void drain()`
- [L78](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L78): `private String failureSummary()`
- [L82](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L82): `private void failed(String key,Exception ex)`
- [L86](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L86): `static void atomicPublish(File directory,Snapshot snapshot) throws IOException`
- [L90](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L90): `static void atomicPublish(File directory,Snapshot snapshot,BeforeReplace beforeReplace) throws IOException`
- [L104](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/LivePapywizard.java#L104): `void awaitIdle(long timeoutMs) throws Exception`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java

- [L146](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L146): `private AlertDialog chooseLanguage()`
- [L163](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L163): `private int dp(int value)`
- [L164](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L164): `private TextView text(String value,int size)`
- [L168](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L168): `private LinearLayout vertical()`
- [L169](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L169): `private GradientDrawable surface(int color)`
- [L170](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L170): `private LinearLayout card(LinearLayout parent,String title)`
- [L175](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L175): `private LinearLayout details(LinearLayout parent,String title)`
- [L181](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L181): `private void primary(Button button)`
- [L185](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L185): `private void buildUi()`
- [L436](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L436): `public void beforeTextChanged(CharSequence s,int start,int count,int after)`
- [L437](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L437): `public void onTextChanged(CharSequence s,int start,int before,int count)`
- [L438](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L438): `public void afterTextChanged(Editable value)`
- [L442](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L442): `public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id)`
- [L443](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L443): `public void onNothingSelected(android.widget.AdapterView<?> p)`
- [L447](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L447): `public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id)`
- [L448](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L448): `public void onNothingSelected(android.widget.AdapterView<?> parent)`
- [L452](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L452): `private PlotWorkspace installPlotWorkspace(android.widget.FrameLayout container,int page,LinearLayout diagram)`
- [L458](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L458): `private void plotControls(LinearLayout parent,PlanPreview view)`
- [L467](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L467): `private void togglePlotWorkspace(PlanPreview view)`
- [L471](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L471): `private void updatePlotSizeLabels()`
- [L476](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L476): `private void syncPlotChrome()`
- [L494](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L494): `private void chooseTiltRange()`
- [L500](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L500): `private void applyTiltRange()`
- [L505](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L505): `private void schedulePlot()`
- [L506](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L506): `private void updateProposal()`
- [L511](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L511): `private String proposalText()`
- [L517](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L517): `private boolean positionReady()`
- [L518](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L518): `private boolean canStartLoadedPlan()`
- [L519](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L519): `private boolean canStartLocalPlan()`
- [L520](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L520): `private void startLoadedPlan()`
- [L529](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L529): `private void abortLoadedPlan()`
- [L532](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L532): `private void prepareRest()`
- [L547](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L547): `private void removePlan()`
- [L555](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L555): `private void clearLocalPlan()`
- [L560](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L560): `private boolean endpointSet(String prefix)`
- [L563](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L563): `private void driveSelection()`
- [L567](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L567): `private void driveEndpoint(String prefix)`
- [L568](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L568): `private void driveUiPoint(double pan,double tilt)`
- [L572](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L572): `private void refreshEndpoints()`
- [L580](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L580): `private Button button(LinearLayout parent,String title,Runnable action,BooleanSupplier enabled)`
- [L589](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L589): `private void arrowButton(LinearLayout parent,String symbol,String description,int axis,int direction)`
- [L619](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L619): `private EditText input(LinearLayout parent,String key,String label,String fallback)`
- [L625](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L625): `public void beforeTextChanged(CharSequence s,int start,int count,int after)`
- [L626](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L626): `public void onTextChanged(CharSequence s,int start,int before,int count)`
- [L627](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L627): `public void afterTextChanged(Editable value)`
- [L631](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L631): `private boolean timingReady()`
- [L634](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L634): `private AlertDialog editRunTiming()`
- [L655](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L655): `private boolean ready()`
- [L656](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L656): `private boolean manualReady()`
- [L657](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L657): `private void selectPage(int index)`
- [L667](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L667): `private String statusText()`
- [L681](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L681): `private void render()`
- [L776](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L776): `private List<RunPlotModel.Capture> warningCaptures()`
- [L782](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L782): `private void showCaptureWarnings()`
- [L787](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L787): `private AlertDialog reviewCaptureWarning(RunPlotModel.Capture capture)`
- [L798](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L798): `private String endpointLabel(String prefix,String title)`
- [L801](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L801): `private String alignmentHelp()`
- [L809](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L809): `private void showColumnRetry()`
- [L820](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L820): `private void showIncident()`
- [L845](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L845): `private String friendlyState(String state)`
- [L848](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L848): `private void safely(Runnable action)`
- [L852](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L852): `private void confirm(String title,String detail,Runnable action)`
- [L855](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L855): `private double number(String key)`
- [L856](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L856): `private int integer(String key)`
- [L857](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L857): `private void saveInputs()`
- [L862](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L862): `private boolean permission()`
- [L872](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L872): `private void refreshDevices()`
- [L882](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L882): `private String deviceName(BluetoothDevice device)`
- [L886](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L886): `private void connect()`
- [L893](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L893): `private void takePosition(String prefix)`
- [L894](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L894): `private void migrateTiltSign()`
- [L908](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L908): `private static long newRunId()`
- [L909](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L909): `private CameraProfile editedProfile()`
- [L910](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L910): `private void calculate()`
- [L920](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L920): `private void loadPlan()`
- [L925](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L925): `private void loadProfiles()`
- [L937](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L937): `private void updateProfilePicker()`
- [L938](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L938): `private void applyProfile()`
- [L944](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L944): `private void saveProfile()`
- [L948](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L948): `private void persistProfiles()`
- [L952](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L952): `private void importConfig()`
- [L953](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L953): `private void parseConfig(InputStream input) throws Exception`
- [L969](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L969): `private void refreshRuns()`
- [L970](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L970): `private void exportCaptures(boolean xml)`
- [L976](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L976): `private void saveCaptureExport(CaptureStore.Run run,boolean xml)`
- [L981](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L981): `private void exportInterruptions()`
- [L986](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L986): `private void exportSupport()`
- [L992](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L992): `private void prepareExport()`
- [L1016](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L1016): `private void migrateTiltDirection()`
- [L1030](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L1030): `private boolean tiltCalibrationReady()`
- [L1033](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L1033): `private void setTiltZero(boolean reset)`
- [L1038](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/MainActivity.java#L1038): `private void setTiltLimits()`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualActionProgress.java

- [L9](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualActionProgress.java#L9): `ManualActionProgress(int commandSequence,String kind,int pan,int tilt,long baseline)`
- [L13](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualActionProgress.java#L13): `Outcome accept(ManualActionStatus status,long version)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualActionStatus.java

- [L8](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualActionStatus.java#L8): `private ManualActionStatus(int sequence,String kind,String result,int pan,int tilt,long duration,long maximum)`
- [L12](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualActionStatus.java#L12): `static ManualActionStatus parse(String[] f)`
- [L30](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualActionStatus.java#L30): `boolean active()`
- [L31](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualActionStatus.java#L31): `boolean succeeded()`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualMotionOptions.java

- [L5](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualMotionOptions.java#L5): `static String[] fields(DeviceState state,int pan,int tilt,int tempo)`
- [L8](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualMotionOptions.java#L8): `static String[] fields(DeviceState state,int pan,int tilt,int panTempo,int tiltTempo)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualMoveProgress.java

- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualMoveProgress.java#L7): `ManualMoveProgress(int baseline, int pan, int tilt)`
- [L10](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualMoveProgress.java#L10): `boolean accept(String[] fields)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualTargets.java

- [L5](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualTargets.java#L5): `private ManualTargets()`
- [L6](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ManualTargets.java#L6): `static int step(int target, int actual, int delta)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/P2Codec.java

- [L10](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/P2Codec.java#L10): `private P2Codec()`
- [L12](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/P2Codec.java#L12): `public static String encode(int sequence, String type, String... fields)`
- [L25](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/P2Codec.java#L25): `public static Frame decode(String raw)`
- [L52](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/P2Codec.java#L52): `public static int crc(String ascii)`
- [L62](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/P2Codec.java#L62): `private static void appendToken(StringBuilder out, String value)`
- [L76](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/P2Codec.java#L76): `Frame(int sequence, String type, String[] fields)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java

- [L10](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java#L10): `private PanoramaPlan(long id, int mode, int x, int y, int cols, int rows, int dx, int dy, int ex, int ey, int settle, int pulse, int exposure, int speedX, int speedY, int policy, int heartbeat)`
- [L32](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java#L32): `public static PanoramaPlan create(long id, boolean spherical, double sx, double sy, double ex, double ey, double fovX, double fovY, double overlap, int settle, int pulse, int exposure, int speed, boolean stopOnLoss)`
- [L59](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java#L59): `public static PanoramaPlan parse(String[] fields)`
- [L70](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java#L70): `public int total()`
- [L75](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java#L75): `public int shots(int band)`
- [L76](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java#L76): `public int[] target(int index)`
- [L90](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java#L90): `public void validate(DeviceState device)`
- [L98](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java#L98): `public String[] fields()`
- [L106](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java#L106): `public boolean sameRecipe(PanoramaPlan other)`
- [L111](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java#L111): `static int shotsInBand(int x, int ex, int tilt, int step)`
- [L122](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaPlan.java#L122): `static int interpolate(int start, int end, int index, int divisor)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java

- [L31](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L31): `String connectionText()`
- [L32](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L32): `String messageText()`
- [L33](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L33): `String diagnosticsText()`
- [L34](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L34): `String incidentDetail()`
- [L35](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L35): `private void setConnection(Supplier<String> text)`
- [L36](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L36): `private void setMessage(Supplier<String> text)`
- [L37](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L37): `private void setDiagnostics(Supplier<String> text)`
- [L38](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L38): `void languageChanged()`
- [L66](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L66): `public void run()`
- [L94](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L94): `String papywizardStatus()`
- [L95](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L95): `void rebuildPapywizard()`
- [L138](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L138): `public List<String> roots()`
- [L139](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L139): `public LivePapywizard.Snapshot snapshot(String key)`
- [L140](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L140): `public void close()`
- [L179](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L179): `private Notification notification()`
- [L186](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L186): `public void observe(Observer observer)`
- [L187](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L187): `public void removeObserver(Observer observer)`
- [L188](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L188): `private void notifyObservers()`
- [L189](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L189): `private void open()`
- [L323](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L323): `private void readDiagnostics(String[] f)`
- [L330](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L330): `private long currentBoot()`
- [L333](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L333): `private void pauseForQuality()`
- [L351](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L351): `private void recordRunFault(String[] fields)`
- [L364](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L364): `void observeRecoveryPause(String[] fields)`
- [L374](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L374): `public boolean canPrepareRest()`
- [L382](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L382): `CaptureStore.Checkpoint restCheckpoint()`
- [L386](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L386): `private CaptureStore.Checkpoint recoveryCheckpoint()`
- [L390](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L390): `boolean hasRecoveryCheckpoint()`
- [L393](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L393): `boolean recoveryInProgress()`
- [L394](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L394): `String recoveryGuidance()`
- [L407](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L407): `private void cancelRest()`
- [L409](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L409): `public void prepareRest(boolean previousColumn,long expectedRun,long expectedIndex)`
- [L422](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L422): `public void run()`
- [L445](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L445): `void finish(Supplier<String> detail)`
- [L451](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L451): `void acceptManualEvent(String[] fields)`
- [L456](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L456): `private void updateManualAction()`
- [L481](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L481): `void commandFeedback(String text, boolean background, boolean success)`
- [L484](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L484): `private void commandFeedback(Supplier<String> text,boolean background,boolean success)`
- [L487](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L487): `public boolean fresh()`
- [L488](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L488): `public boolean manualAllowed()`
- [L489](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L489): `public boolean navigationAllowed()`
- [L490](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L490): `boolean connectedTo(String address)`
- [L491](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L491): `private void signalQualityWarning()`
- [L498](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L498): `public String navigationReason()`
- [L520](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L520): `static String faultDescription(String code)`
- [L530](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L530): `private void saveIncident()`
- [L537](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L537): `private void raiseIncident(String code,Supplier<String> detail)`
- [L540](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L540): `private void raiseIncident(String code,Supplier<String> text,boolean requiresStop)`
- [L550](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L550): `public void acknowledgeIncident(long id)`
- [L554](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L554): `public void recoverDevice()`
- [L563](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L563): `private void beginRecovery()`
- [L573](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L573): `public void run()`
- [L595](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L595): `public boolean hasConnection()`
- [L596](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L596): `public void disconnect()`
- [L613](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L613): `private void requireReady()`
- [L624](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L624): `public CompletableFuture<V2Client.Result> command(String type, String... fields)`
- [L628](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L628): `private CompletableFuture<V2Client.Result> execute(String type, String[] fields, boolean emergency)`
- [L631](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L631): `private CompletableFuture<V2Client.Result> execute(String type, String[] fields, boolean emergency, boolean background)`
- [L671](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L671): `private static String actionName(String type)`
- [L693](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L693): `private static String resultName(String code)`
- [L722](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L722): `public void emergencyStop()`
- [L732](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L732): `public void setTiltZero(boolean reset)`
- [L746](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L746): `public void move(double panDelta, double tiltDelta)`
- [L758](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L758): `public void setManualTempo(int tempo)`
- [L762](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L762): `public void setManualTempos(int panTempo,int tiltTempo)`
- [L769](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L769): `private String[] manualMoveFields(int x,int y)`
- [L775](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L775): `public boolean jogAllowed()`
- [L776](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L776): `public void setJogSpeed(double speed)`
- [L779](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L779): `public void setJogSpeed(int axis,double speed)`
- [L786](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L786): `public void setRememberSettleMillis(int millis)`
- [L790](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L790): `private void deferEndpointCapture()`
- [L798](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L798): `public boolean endpointCaptureAllowed()`
- [L802](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L802): `public boolean jogHeldFor(int axis,int direction)`
- [L803](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L803): `public void beginJog(int axis,int direction)`
- [L828](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L828): `public void endJog()`
- [L842](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L842): `public void moveToUi(double pan,double tilt)`
- [L847](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L847): `private void prepareMove(int x,int y)`
- [L880](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L880): `private void sendManualMove(int x,int y)`
- [L897](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L897): `private void cancelPreparation()`
- [L898](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L898): `private void finishPreparation()`
- [L903](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L903): `public void trigger(int pulse)`
- [L926](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L926): `public void loadPlan(PanoramaPlan plan)`
- [L937](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L937): `public void runCommand(String type)`
- [L956](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L956): `public void abortPlan()`
- [L959](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L959): `private void markDraftForReview()`
- [L962](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L962): `public void confirmDraftReference()`
- [L966](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L966): `public void discardDraft()`
- [L971](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L971): `public void abortPlan(boolean discardDraftWhenIdle)`
- [L978](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L978): `public void run()`
- [L1004](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L1004): `public void columns(boolean forward)`
- [L1013](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L1013): `String timingSummary()`
- [L1024](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L1024): `public void saveWarningTolerances(double pan,double tilt)`
- [L1033](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L1033): `boolean canConfigureStability()`
- [L1040](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L1040): `void observeStabilitySettings()`
- [L1046](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L1046): `public void readCaptureStability()`
- [L1051](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L1051): `public void saveCaptureStability(double span,double pan,double tilt,double seconds)`
- [L1071](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L1071): `boolean canRetryColumn()`
- [L1075](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L1075): `public void retryColumn(boolean previous,long expectedRun,long expectedIndex)`
- [L1081](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L1081): `public void updateTiming(int settle, int pulse, int exposure)`
- [L1092](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PanoramaService.java#L1092): `public void timelapse(long id, int interval, int pulse)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanAbort.java

- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanAbort.java#L7): `PlanAbort(String state)`
- [L11](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanAbort.java#L11): `static boolean allowed(String state)`
- [L15](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanAbort.java#L15): `String next(String state,boolean fresh)`
- [L22](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanAbort.java#L22): `void acknowledged(boolean accepted)`
- [L27](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanAbort.java#L27): `boolean done()`
- [L28](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanAbort.java#L28): `boolean failed()`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java

- [L35](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L35): `PlanPreview(Context context)`
- [L63](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L63): `void setListener(EndpointListener value)`
- [L64](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L64): `void setWarningListener(java.util.function.Consumer<RunPlotModel.Capture> value)`
- [L65](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L65): `void setProposalListener(Runnable value)`
- [L66](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L66): `void setDriveAction(Runnable value)`
- [L67](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L67): `void setDriveReady(boolean ready)`
- [L68](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L68): `void setEditMode(int mode)`
- [L69](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L69): `int editMode()`
- [L70](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L70): `void discardSelection()`
- [L71](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L71): `void commitSelection()`
- [L76](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L76): `void fineTilt(double delta)`
- [L82](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L82): `void finePan(double delta)`
- [L83](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L83): `private double basePan()`
- [L84](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L84): `private double baseTilt()`
- [L85](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L85): `void setEndpoints(double a,double b,double c,double d)`
- [L89](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L89): `void setEndpointAvailability(boolean start,boolean end)`
- [L90](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L90): `private void propose(double pan,double tilt)`
- [L98](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L98): `void setPlan(PanoramaPlan value)`
- [L105](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L105): `private boolean positionKnown()`
- [L106](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L106): `private int currentPan()`
- [L107](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L107): `private int currentTilt()`
- [L108](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L108): `void followCurrentPosition()`
- [L116](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L116): `void setLive(DeviceState value,RunPlotModel model,boolean current)`
- [L121](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L121): `void zoom(double factor)`
- [L122](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L122): `int alignmentHeight()`
- [L128](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L128): `void wholeRange()`
- [L135](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L135): `void alignmentRange(double tiltExtent)`
- [L143](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L143): `void fit()`
- [L164](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L164): `void save(Bundle b,String key)`
- [L168](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L168): `void restore(Bundle b,String key)`
- [L178](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L178): `private float density()`
- [L179](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L179): `private int dp(int value)`
- [L180](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L180): `private void updateSelectionActions()`
- [L204](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L204): `private float l()`
- [L205](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L205): `private float t()`
- [L206](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L206): `private float w()`
- [L207](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L207): `private float h()`
- [L208](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L208): `private float px(double x)`
- [L209](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L209): `private float py(double y)`
- [L210](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlanPreview.java#L210): `private void dot(Canvas c,int x,int y,int color,float radius)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotViewport.java

- [L6](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotViewport.java#L6): `double pan(double fraction)`
- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotViewport.java#L7): `double tilt(double fraction)`
- [L8](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotViewport.java#L8): `double x(double pan)`
- [L9](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotViewport.java#L9): `double y(double tilt)`
- [L10](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotViewport.java#L10): `void fit(double minPan,double maxPan,double minTilt,double maxTilt)`
- [L14](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotViewport.java#L14): `void zoom(double factor)`
- [L20](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotViewport.java#L20): `void keepVisible(double pan,double tilt)`
- [L27](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotViewport.java#L27): `void shift(double dx,double dy)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotWorkspace.java

- [L15](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotWorkspace.java#L15): `PlotWorkspace(Context context,LinearLayout diagram,ScrollView settings)`
- [L21](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotWorkspace.java#L21): `boolean expanded()`
- [L22](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/PlotWorkspace.java#L22): `void setExpanded(boolean value)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RasterRecovery.java

- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RasterRecovery.java#L7): `RasterRecovery(PanoramaPlan source,long nextIndex)`
- [L12](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RasterRecovery.java#L12): `static boolean recoverable(String error)`
- [L22](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RasterRecovery.java#L22): `static boolean uncertainExposure(String error)`
- [L27](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RasterRecovery.java#L27): `String unit()`
- [L28](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RasterRecovery.java#L28): `int startColumn(boolean previous)`
- [L44](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RasterRecovery.java#L44): `PanoramaPlan remaining(long newId,boolean previous)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ReconnectPolicy.java

- [L8](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ReconnectPolicy.java#L8): `void connected()`
- [L9](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ReconnectPolicy.java#L9): `void stop()`
- [L10](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/ReconnectPolicy.java#L10): `int nextAttempt()`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RecoveryFlow.java

- [L10](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RecoveryFlow.java#L10): `RecoveryFlow(long now)`
- [L11](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RecoveryFlow.java#L11): `String next(String state,long run,long version,boolean fresh,long now)`
- [L36](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RecoveryFlow.java#L36): `void acknowledged(boolean accepted,long now)`
- [L43](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RecoveryFlow.java#L43): `boolean done()`
- [L44](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RecoveryFlow.java#L44): `boolean failed()`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RestPreparation.java

- [L9](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RestPreparation.java#L9): `RestPreparation(long sourceRun,long newRun)`
- [L10](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RestPreparation.java#L10): `RestPreparation(long sourceRun,long newRun,boolean clearedAlready)`
- [L11](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RestPreparation.java#L11): `String next(String state,long run,long version,boolean fresh,boolean recipeMatches)`
- [L34](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RestPreparation.java#L34): `void acknowledged(boolean accepted)`
- [L41](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RestPreparation.java#L41): `boolean done()`
- [L42](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RestPreparation.java#L42): `boolean failed()`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunPlotModel.java

- [L11](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunPlotModel.java#L11): `Capture(String key,int tx,int ty,int x,int y,boolean inferred)`
- [L14](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunPlotModel.java#L14): `Capture(String key,int tx,int ty,int x,int y,boolean inferred,int flags,long ordinal)`
- [L18](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunPlotModel.java#L18): `static Capture parse(String key,String payload)`
- [L21](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunPlotModel.java#L21): `static Capture parse(String key,String payload,int flags)`
- [L30](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunPlotModel.java#L30): `Sample(int x,int y,boolean joined)`
- [L37](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunPlotModel.java#L37): `void status(P2Codec.Frame frame,long now)`
- [L46](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunPlotModel.java#L46): `void disconnected()`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunRecoveryPause.java

- [L5](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunRecoveryPause.java#L5): `static boolean reason(String error)`
- [L10](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunRecoveryPause.java#L10): `static boolean active(DeviceState s)`
- [L13](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/RunRecoveryPause.java#L13): `static String detail(DeviceState s)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/StatusLogPolicy.java

- [L7](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/StatusLogPolicy.java#L7): `boolean log(String[] f,long now)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/SupportLog.java

- [L17](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/SupportLog.java#L17): `SupportLog(File directory)`
- [L18](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/SupportLog.java#L18): `SupportLog(File directory,long maximumBytes,int retainedFiles)`
- [L22](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/SupportLog.java#L22): `private File part(int index)`
- [L25](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/SupportLog.java#L25): `void append(String line)`
- [L38](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/SupportLog.java#L38): `String snapshot() throws IOException`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java

- [L649](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L649): `Language(String tag,String label,Locale locale)`
- [L650](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L650): `static Language fromTag(String tag)`
- [L656](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L656): `UiText(String german,String english)`
- [L657](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L657): `String text()`
- [L658](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L658): `String in(Language language)`
- [L659](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L659): `static Language language()`
- [L660](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L660): `static Locale locale()`
- [L661](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L661): `static void select(String tag)`
- [L663](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L663): `static String existing(String value)`
- [L671](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L671): `Message(String german,String english)`
- [L672](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L672): `String text()`
- [L673](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L673): `String in(Language language)`
- [L675](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L675): `static Message capture(Supplier<String> text)`
- [L678](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/UiText.java#L678): `private static String render(Language language,Supplier<String> text)`

### PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java

- [L13](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L13): `void write(String frame) throws IOException`
- [L15](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L15): `default void abort()`
- [L21](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L21): `Result(boolean accepted, String code)`
- [L22](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L22): `Result(boolean accepted, String code,int commandSequence)`
- [L39](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L39): `public V2Client(Transport transport, ScheduledExecutorService worker, int firstSequence, long timeoutMs, int retries)`
- [L48](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L48): `public synchronized CompletableFuture<Result> send(String type, String... fields)`
- [L54](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L54): `public synchronized CompletableFuture<Result> emergencyStop(String runId)`
- [L60](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L60): `public synchronized CompletableFuture<Result> stopJog(String token)`
- [L66](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L66): `private CompletableFuture<Result> begin(String type, String[] fields)`
- [L76](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L76): `private void transmit(Pending request)`
- [L96](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L96): `private boolean write(String wire, Pending request)`
- [L117](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L117): `private void failWrite(Object token, IOException error)`
- [L129](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L129): `public synchronized void receive(P2Codec.Frame frame)`
- [L140](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L140): `public synchronized void heartbeat(String runId)`
- [L146](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L146): `public synchronized boolean busy()`
- [L147](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L147): `private int nextSequence()`
- [L148](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L148): `private static CompletableFuture<Result> failed(String message)`
- [L152](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L152): `private void cancelPending(String reason)`
- [L165](../../PanoramaAndroid/app/src/main/java/de/tomnaiser/panoramaandroid/V2Client.java#L165): `Pending(int sequence, String wire, String type)`

## Windows

### PanoramaControlGitRepo/PanoramaControl/Application/DeviceIdentity.cs

- [L11](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/DeviceIdentity.cs#L11): `public static string Resolve(string port)`
- [L39](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/DeviceIdentity.cs#L39): `public static bool Confirmed(string identity)`

### PanoramaControlGitRepo/PanoramaControl/Application/DiagnosticSessionLog.cs

- [L21](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/DiagnosticSessionLog.cs#L21): `public DiagnosticSessionLog(string directory = null)`
- [L29](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/DiagnosticSessionLog.cs#L29): `public void Write(string direction, string detail, string data)`
- [L36](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/DiagnosticSessionLog.cs#L36): `private StreamWriter Open()`
- [L38](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/DiagnosticSessionLog.cs#L38): `private void Drain()`
- [L72](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/DiagnosticSessionLog.cs#L72): `public Task FlushAsync()`
- [L80](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/DiagnosticSessionLog.cs#L80): `public void Dispose()`

### PanoramaControlGitRepo/PanoramaControl/Application/LiveRunBackup.cs

- [L27](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/LiveRunBackup.cs#L27): `public LiveRunBackup(RunStore store, string directory)`
- [L40](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/LiveRunBackup.cs#L40): `public void Rebuild()`
- [L41](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/LiveRunBackup.cs#L41): `public void Request(string key)`
- [L50](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/LiveRunBackup.cs#L50): `private void Drain()`
- [L81](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/LiveRunBackup.cs#L81): `public static void AtomicPublish(string path, string xml)`
- [L94](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/LiveRunBackup.cs#L94): `public Task WhenIdleAsync()`
- [L95](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/LiveRunBackup.cs#L95): `public void Dispose()`

### PanoramaControlGitRepo/PanoramaControl/Application/ManualMoveTargets.cs

- [L14](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/ManualMoveTargets.cs#L14): `public void Reset()`
- [L16](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/ManualMoveTargets.cs#L16): `public void ObserveActual(double pan, double tilt)`
- [L30](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/ManualMoveTargets.cs#L30): `public double StepPan(double delta)`
- [L31](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/ManualMoveTargets.cs#L31): `public double StepTilt(double delta)`
- [L33](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/ManualMoveTargets.cs#L33): `private static double Step(double target, double actual, double delta)`
- [L40](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/ManualMoveTargets.cs#L40): `public void Accept(double pan, double tilt)`

### PanoramaControlGitRepo/PanoramaControl/Application/PanoramaPlanFactory.cs

- [L8](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaPlanFactory.cs#L8): `public static PanoramaPlan FromCurrentParameters(uint runId, DisconnectPolicy disconnectPolicy, uint heartbeatTimeoutMs)`
- [L37](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaPlanFactory.cs#L37): `private static PanoramaPlan CreateSpherical(uint runId, DisconnectPolicy disconnectPolicy, uint heartbeatTimeoutMs)`
- [L68](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaPlanFactory.cs#L68): `private static ushort SpeedToPermille(double speed)`

### PanoramaControlGitRepo/PanoramaControl/Application/PanoramaSessionController.cs

- [L40](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L40): `public PanoramaSessionController(ISerialTransport transport, string logDirectory = null)`
- [L70](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L70): `public void RecordUserAction(string action, string detail)`
- [L73](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L73): `public async Task ConnectAsync(string portName, int baudRate, CancellationToken cancellationToken)`
- [L83](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L83): `public void Disconnect()`
- [L97](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L97): `public async Task LoadAndStartAsync(PanoramaPlan plan, CancellationToken cancellationToken)`
- [L103](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L103): `public async Task LoadPlanAsync(PanoramaPlan plan, CancellationToken cancellationToken)`
- [L120](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L120): `public async Task<CommandResult> StartAsync(uint runId, CancellationToken cancellationToken)`
- [L131](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L131): `public Task<CommandResult> PauseAsync(uint runId, CancellationToken cancellationToken)`
- [L138](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L138): `public Task<CommandResult> ResumeAsync(uint runId, CancellationToken cancellationToken)`
- [L145](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L145): `public Task<CommandResult> RewindColumnsAsync(uint runId, ushort columns, CancellationToken cancellationToken)`
- [L153](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L153): `public Task<CommandResult> ForwardColumnsAsync(uint runId, ushort columns, CancellationToken cancellationToken)`
- [L161](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L161): `public async Task<CommandResult> UpdateTimingAsync(uint runId, RunTimingSettings timing, CancellationToken cancellationToken)`
- [L177](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L177): `public Task<CommandResult> StopAsync(uint runId, CancellationToken cancellationToken)`
- [L184](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L184): `public Task<CommandResult> RefreshCapabilitiesAsync(CancellationToken cancellationToken)`
- [L190](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L190): `public Task<CommandResult> SetTiltLimitsAsync(AngleMdeg minimum, AngleMdeg maximum, CancellationToken cancellationToken)`
- [L198](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L198): `public async Task<CommandResult> StartTimelapseAsync(uint runId, uint intervalMs, uint pulseMs, CancellationToken cancellationToken)`
- [L210](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L210): `public Task<CommandResult> StopTimelapseAsync(uint runId, CancellationToken cancellationToken)`
- [L216](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L216): `public Task<CommandResult> EmergencyStopAsync(uint runId, string reason, CancellationToken cancellationToken)`
- [L226](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L226): `public Task<CommandResult> ManualMoveAsync(AngleMdeg pan, AngleMdeg tilt, CancellationToken cancellationToken)`
- [L238](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L238): `public Task<CommandResult> TriggerOnceAsync(uint pulseTimeMs, CancellationToken cancellationToken)`
- [L247](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L247): `public Task<CommandResult> ClearRunAsync(CancellationToken cancellationToken)`
- [L253](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L253): `public Task<CommandResult> RequestStatusAsync(CancellationToken cancellationToken)`
- [L259](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L259): `public Task<CommandResult> RequestDiagnosticsAsync(CancellationToken cancellationToken)`
- [L265](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L265): `public Task<CommandResult> RequestDebugLogAsync(ushort afterSequence, CancellationToken cancellationToken)`
- [L271](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L271): `public Task<CommandResult> RequestSupportBundleAsync(CancellationToken cancellationToken)`
- [L277](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L277): `private async Task<CommandResult> ReadDiagnosticTransferAsync(string command, CancellationToken token, params string[] fields)`
- [L318](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L318): `private async Task EnsureIdleAsync(CancellationToken cancellationToken)`
- [L333](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L333): `private async Task ConnectAndHandshakeAsync(bool reconnecting, CancellationToken cancellationToken)`
- [L384](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L384): `private void StartReconnectLoop()`
- [L422](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L422): `private void StartHeartbeat()`
- [L446](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L446): `private void StopHeartbeat()`
- [L453](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L453): `private void ProtocolFrameReceived(object sender, ProtocolFrameEventArgs eventArgs)`
- [L474](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L474): `default: AcceptFeatureFrame(frame)`
- [L484](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L484): `private void ParseCapabilities(ProtocolFrame frame)`
- [L500](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L500): `private void ParseDeviceInfo(ProtocolFrame frame)`
- [L524](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L524): `private void ParseDiagnostics(ProtocolFrame frame)`
- [L559](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L559): `private void ParseStatus(ProtocolFrame frame)`
- [L609](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L609): `private void ParseDebugEvent(ProtocolFrame frame)`
- [L620](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L620): `private static string DebugEventName(string code)`
- [L650](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L650): `private void ParseDebugLogEnd(ProtocolFrame frame)`
- [L659](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L659): `private void ScheduleSupportSnapshot()`
- [L676](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L676): `private void ParsePlan(ProtocolFrame frame)`
- [L717](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L717): `private void ParseRunSegment(ProtocolFrame frame)`
- [L733](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L733): `private void ParseRunEvent(ProtocolFrame frame, string eventType)`
- [L768](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L768): `private void ParseManualRunEvent(ProtocolFrame frame)`
- [L798](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L798): `private void ReconstructMissingEvents(DeviceStatus status)`
- [L803](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L803): `private void ReconstructMissingEventsUntil(uint runId, uint captureCount, uint eventSequence)`
- [L840](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L840): `private void UpdateCurrentSegment(DeviceStatus status)`
- [L860](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L860): `private void TransportStateChanged(object sender, TransportStateChangedEventArgs eventArgs)`
- [L876](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L876): `private void TransportError(object sender, TransportErrorEventArgs eventArgs)`
- [L882](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L882): `private void ProtocolError(object sender, ProtocolErrorEventArgs eventArgs)`
- [L889](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L889): `private void ProtocolTrace(object sender, ProtocolTraceEventArgs eventArgs)`
- [L894](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L894): `private void SetConnectionState(ConnectionState state)`
- [L903](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L903): `private void RaiseMessage(string message)`
- [L910](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L910): `private static void EnsureAccepted(CommandResult result, string operation)`
- [L915](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L915): `private void EnsureConnected()`
- [L920](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L920): `private static string SanitizeReason(string reason)`
- [L926](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L926): `private static int ParseInt(string value)`
- [L927](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L927): `private static uint ParseUInt(string value)`
- [L928](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L928): `private static ushort ParseUShort(string value)`
- [L930](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L930): `private static RunState ParseRunState(string value)`
- [L935](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L935): `private static RunPhase ParseRunPhase(string value)`
- [L941](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L941): `private static string ToPascalCase(string value)`
- [L952](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L952): `private static string[] ToArray(System.Collections.Generic.IReadOnlyList<string> values)`
- [L959](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L959): `private void ThrowIfDisposed()`
- [L964](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L964): `public void Dispose()`
- [L991](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L991): `public SessionMessageEventArgs(string message)`
- [L997](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L997): `public ConnectionStateEventArgs(ConnectionState state)`
- [L1003](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L1003): `public DeviceStatusEventArgs(DeviceStatus status)`
- [L1009](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L1009): `public DeviceCapabilitiesEventArgs(DeviceCapabilities capabilities)`
- [L1015](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L1015): `public DeviceInfoEventArgs(DeviceInfo deviceInfo)`
- [L1021](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L1021): `public DeviceDiagnosticsEventArgs(DeviceDiagnostics diagnostics)`
- [L1027](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.cs#L1027): `public RunEventEventArgs(RunEvent runEvent)`

### PanoramaControlGitRepo/PanoramaControl/Application/PanoramaSessionController.Operations.cs

- [L51](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L51): `private bool Fresh(DateTime received)`
- [L53](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L53): `private static bool AxisSafe(int value, int min, int max)`
- [L88](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L88): `private void EnsureReady()`
- [L89](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L89): `private void EnsureAcquisitionReady()`
- [L90](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L90): `private void EnsureNavigation()`
- [L91](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L91): `private void EnsureColumnsReady(uint runId)`
- [L99](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L99): `private async Task<CommandResult> SendCommandAsync(string type, CancellationToken token, params string[] fields)`
- [L131](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L131): `private void ResetConnectionData()`
- [L139](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L139): `private void ForgetManualAction()`
- [L147](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L147): `private void CheckManualDeadline()`
- [L156](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L156): `private async Task WaitForStatusAsync(CancellationToken token)`
- [L166](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L166): `public async Task RequestFreshStatusAsync(CancellationToken token)`
- [L179](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L179): `private void AcceptDeviceSession(ProtocolFrame frame)`
- [L194](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L194): `private void AcceptFeatureFrame(ProtocolFrame frame)`
- [L227](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L227): `public async Task RecoverAsync(CancellationToken token)`
- [L262](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L262): `public async Task AbortPlanAsync(CancellationToken token)`
- [L276](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L276): `private async Task WaitForStateAsync(RunState state, CancellationToken token)`
- [L287](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L287): `public async Task SetTiltZeroAsync(bool reset, CancellationToken token)`
- [L295](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L295): `public Task<CommandResult> SaveWarningThresholdsAsync(int pan, int tilt, CancellationToken token)`
- [L302](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L302): `public void ValidateManualTarget(int pan, int tilt)`
- [L312](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L312): `private static bool ManualAxisAllowed(int actual, int target, int min, int max)`
- [L315](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L315): `public async Task BeginJogAsync(int axis, int direction, int velocityMdeg, CancellationToken token)`
- [L331](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L331): `private async void KeepJogAlive(uint jog)`
- [L343](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L343): `public async Task EndJogAsync(CancellationToken token)`
- [L349](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L349): `private void StopJogTimer()`
- [L350](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L350): `private void AcceptCaptureWarning(ProtocolFrame frame)`
- [L359](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L359): `private void ApplyKnownWarning(RunEvent capture)`
- [L364](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Operations.cs#L364): `public CaptureWarningEventArgs(uint run, uint capture, int flags)`

### PanoramaControlGitRepo/PanoramaControl/Application/PanoramaSessionController.Parity.cs

- [L29](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L29): `public Task FlushDiagnosticsAsync()`
- [L30](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L30): `public void AttachArchive(RunStore store)`
- [L35](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L35): `public string ArchiveKey(uint run)`
- [L39](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L39): `private void ReconcileRest()`
- [L50](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L50): `private void ArchiveAction(Action action)`
- [L60](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L60): `private void DrainArchive()`
- [L75](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L75): `private void ArchivePlan(PanoramaPlan plan)`
- [L85](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L85): `private void ArchiveCaptureEvent(RunEvent capture)`
- [L104](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L104): `private void SaveRecoveryCheckpoint(string overrideError = null)`
- [L139](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L139): `public async Task PrepareRestAsync(bool previous, uint expectedRun, uint expectedIndex, CancellationToken token)`
- [L188](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L188): `private void VerifyRestReference(RecoveryCheckpoint checkpoint, int epoch)`
- [L194](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L194): `private async Task AwaitConfirmedState(RunState state, uint run, CancellationToken token)`
- [L206](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L206): `public async Task SaveStabilityAsync(CaptureStabilitySettings request, CancellationToken token)`
- [L220](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L220): `public Task ReadStabilityAsync(CancellationToken token)`
- [L221](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L221): `private async Task ReadStabilityCore(CancellationToken token)`
- [L229](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L229): `public async Task RetryColumnAsync(bool previous, CancellationToken token)`
- [L238](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PanoramaSessionController.Parity.cs#L238): `private void ObserveRetainedPause()`

### PanoramaControlGitRepo/PanoramaControl/Application/PapyWizardRunExporter.cs

- [L9](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs#L9): `void Begin()`
- [L10](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs#L10): `void Write(RunEvent imageEvent, uint pictureId)`
- [L11](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs#L11): `void End()`
- [L27](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs#L27): `public PapyWizardRunExporter()`
- [L31](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs#L31): `public PapyWizardRunExporter(IPapyWizardWriter writer)`
- [L38](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs#L38): `public void BeginRun()`
- [L50](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs#L50): `public bool TryExport(RunEvent imageEvent, out uint pictureId)`
- [L74](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs#L74): `public void CompleteRun()`
- [L86](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs#L86): `public void Begin()`
- [L87](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs#L87): `public void Write(RunEvent imageEvent, uint pictureId)`
- [L94](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/PapyWizardRunExporter.cs#L94): `public void End()`

### PanoramaControlGitRepo/PanoramaControl/Application/RunArchive.cs

- [L22](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L22): `public RunArchive(string directory)`
- [L23](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L23): `public string SelectRun(string device, uint boot, uint run)`
- [L29](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L29): `private string PathFor(string key)`
- [L34](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L34): `private static string Number(object value)`
- [L35](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L35): `private static string Checksum(string value)`
- [L39](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L39): `private void Append(string key, params object[] values)`
- [L45](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L45): `public void StorePlan(string key, PanoramaPlan plan)`
- [L54](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L54): `public void Store(string key, RunEvent e)`
- [L68](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L68): `public void Warn(string key, uint capture, int flags)`
- [L69](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L69): `private static string Identity(RunEvent e)`
- [L70](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L70): `private static uint U(string s)`
- [L71](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L71): `private static int I(string s)`
- [L72](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L72): `public RunEvent[] Snapshot(string key)`
- [L99](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L99): `private IEnumerable<string[]> Records(string key)`
- [L111](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L111): `public string[] PlanFields(string key)`
- [L112](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L112): `public static void Export(RunEvent[] snapshot, string xmlPath, string csvPath)`
- [L136](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L136): `public LocalSettings(string path)`
- [L143](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L143): `public string Get(string key, string fallback)`
- [L144](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunArchive.cs#L144): `public void Set(string key, string value)`

### PanoramaControlGitRepo/PanoramaControl/Application/RunExport.cs

- [L12](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunExport.cs#L12): `private static string N(object value)`
- [L13](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunExport.cs#L13): `public static string Xml(RunEvent[] snapshot, int roll)`
- [L25](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunExport.cs#L25): `public static string Csv(RunEvent[] snapshot)`

### PanoramaControlGitRepo/PanoramaControl/Application/RunRecovery.cs

- [L27](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunRecovery.cs#L27): `public static bool SameRecipe(PanoramaPlan a, PanoramaPlan b)`
- [L29](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunRecovery.cs#L29): `public static bool RetainedPause(string error)`
- [L33](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunRecovery.cs#L33): `public static bool Recoverable(string error)`
- [L37](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunRecovery.cs#L37): `public static bool UncertainExposure(string error)`
- [L41](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunRecovery.cs#L41): `public static int StartUnit(PanoramaPlan plan, uint next, bool previous)`
- [L58](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunRecovery.cs#L58): `public static PanoramaPlan Remaining(PanoramaPlan source, uint next, bool previous, uint newRun)`
- [L82](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunRecovery.cs#L82): `public CaptureStabilitySettings(int span, int pan, int tilt, int wait, uint revision)`
- [L88](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunRecovery.cs#L88): `public string[] Fields()`
- [L93](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunRecovery.cs#L93): `public bool SameValues(CaptureStabilitySettings other)`

### PanoramaControlGitRepo/PanoramaControl/Application/RunStore.cs

- [L20](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L20): `private static string Q(string s)`
- [L21](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L21): `private static string N(object value)`
- [L22](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L22): `public RunStore(string directory)`
- [L48](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L48): `public string SelectRun(string device, uint boot, uint run)`
- [L54](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L54): `public string KeyFor(string device, uint boot, uint run)`
- [L55](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L55): `private void Ensure(string key)`
- [L56](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L56): `private void Touch(string key)`
- [L57](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L57): `private void Notify(string key)`
- [L60](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L60): `public string Root(string key)`
- [L73](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L73): `public void StorePlan(string key, PanoramaPlan plan)`
- [L86](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L86): `public string[] PlanFields(string key)`
- [L88](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L88): `internal static string Encode(RunEvent e)`
- [L93](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L93): `internal static RunEvent Decode(string payload)`
- [L103](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L103): `private static string Identity(RunEvent e)`
- [L104](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L104): `private bool StoreCore(string key, RunEvent incoming)`
- [L117](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L117): `public void Store(string key, RunEvent e)`
- [L119](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L119): `public void Warn(string key, uint capture, int flags)`
- [L131](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L131): `public RunEvent[] Snapshot(string key)`
- [L141](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L141): `public RunEvent[] LinkedSnapshot(string key)`
- [L153](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L153): `public void SaveCheckpoint(RecoveryCheckpoint checkpoint)`
- [L164](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L164): `public void Complete(string key)`
- [L165](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L165): `public RecoveryCheckpoint Checkpoint(string device, uint run)`
- [L175](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L175): `public RestReservation PendingRest(string parent)`
- [L184](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L184): `public RestReservation PendingChild(string child)`
- [L192](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L192): `public bool IsRest(string key)`
- [L193](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L193): `public bool CanStartRest(string key, PanoramaPlan plan, string device, uint boot, int tilt)`
- [L202](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L202): `public void ReserveRest(RecoveryCheckpoint checkpoint, RestReservation rest)`
- [L217](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L217): `public void ConfirmRest(RestReservation rest)`
- [L222](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L222): `public int Roll(string key)`
- [L223](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L223): `public void SetRoll(string key, int roll)`
- [L228](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L228): `public string Faults(string key)`
- [L236](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L236): `private void ImportJournals()`
- [L259](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunStore.cs#L259): `public void Dispose()`

### PanoramaControlGitRepo/PanoramaControl/Application/RunTimingSettings.cs

- [L7](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunTimingSettings.cs#L7): `private RunTimingSettings(uint settleTimeMs, uint triggerPulseMs, uint exposureWaitMs)`
- [L18](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunTimingSettings.cs#L18): `public static RunTimingSettings FromSeconds(double moveDelay, double exposureDelay, double triggerDuration, bool controlTriggerDuration)`
- [L30](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/RunTimingSettings.cs#L30): `private static uint ToMilliseconds(double seconds, uint maximumMs, string name)`

### PanoramaControlGitRepo/PanoramaControl/Application/SqliteDatabase.cs

- [L15](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L15): `private delegate int RowCallback(IntPtr context, int count, IntPtr values, IntPtr names)`
- [L18](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L18): `private static extern int sqlite3_open_v2(byte[] path, out IntPtr db, int flags, IntPtr vfs)`
- [L21](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L21): `private static extern int sqlite3_exec(IntPtr db, byte[] sql, RowCallback callback, IntPtr context, out IntPtr error)`
- [L24](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L24): `private static extern int sqlite3_close_v2(IntPtr db)`
- [L27](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L27): `private static extern void sqlite3_free(IntPtr pointer)`
- [L28](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L28): `private static byte[] Utf8(string value)`
- [L29](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L29): `private static string Read(IntPtr pointer)`
- [L36](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L36): `internal SqliteDatabase(string path)`
- [L44](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L44): `internal static string Quote(string value)`
- [L50](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L50): `internal List<string[]> Query(string sql)`
- [L71](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L71): `internal void Execute(string sql)`
- [L72](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L72): `internal void Transaction(Action action)`
- [L78](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/SqliteDatabase.cs#L78): `public void Dispose()`

### PanoramaControlGitRepo/PanoramaControl/Application/UiText.Catalogue.cs

- [L6](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/UiText.Catalogue.cs#L6): `private static Dictionary<string,string> CreateCatalogue()`

### PanoramaControlGitRepo/PanoramaControl/Application/UiText.cs

- [L16](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/UiText.cs#L16): `private static string PhrasePattern(string text)`
- [L21](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/UiText.cs#L21): `public static string T(string text)`
- [L27](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/UiText.cs#L27): `public static string Source(string text)`

### PanoramaControlGitRepo/PanoramaControl/Application/UiText.Windows.cs

- [L6](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Application/UiText.Windows.cs#L6): `private static void AddWindows(Dictionary<string,string> values)`

### PanoramaControlGitRepo/PanoramaControl/CalculationsHelper.cs

- [L11](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/CalculationsHelper.cs#L11): `public static string CalcFOV()`

### PanoramaControlGitRepo/PanoramaControl/Communication/BluetoothComTransport.cs

- [L35](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/BluetoothComTransport.cs#L35): `public void Connect(string portName, int baudRate)`
- [L66](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/BluetoothComTransport.cs#L66): `public void Disconnect()`
- [L78](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/BluetoothComTransport.cs#L78): `public void Send(string text)`
- [L98](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/BluetoothComTransport.cs#L98): `private void PortDataReceived(object sender, SerialDataReceivedEventArgs eventArgs)`
- [L145](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/BluetoothComTransport.cs#L145): `private void ClosePortLocked()`
- [L156](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/BluetoothComTransport.cs#L156): `private void RaiseStateChanged(bool connected)`
- [L162](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/BluetoothComTransport.cs#L162): `private void RaiseError(string operation, Exception exception)`
- [L168](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/BluetoothComTransport.cs#L168): `private void ThrowIfDisposed()`
- [L173](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/BluetoothComTransport.cs#L173): `public void Dispose()`

### PanoramaControlGitRepo/PanoramaControl/Communication/ISerialTransport.cs

- [L12](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/ISerialTransport.cs#L12): `void Connect(string portName, int baudRate)`
- [L13](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/ISerialTransport.cs#L13): `void Disconnect()`
- [L14](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/ISerialTransport.cs#L14): `void Send(string text)`
- [L19](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/ISerialTransport.cs#L19): `public SerialLineReceivedEventArgs(string line)`
- [L29](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/ISerialTransport.cs#L29): `public TransportStateChangedEventArgs(bool connected)`
- [L39](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Communication/ISerialTransport.cs#L39): `public TransportErrorEventArgs(string operation, Exception exception)`

### PanoramaControlGitRepo/PanoramaControl/Domain/AngleMdeg.cs

- [L11](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/AngleMdeg.cs#L11): `public AngleMdeg(int value)`
- [L18](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/AngleMdeg.cs#L18): `public static AngleMdeg FromDegrees(double degrees)`
- [L26](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/AngleMdeg.cs#L26): `public double ToDegrees()`
- [L31](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/AngleMdeg.cs#L31): `public int CompareTo(AngleMdeg other)`
- [L36](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/AngleMdeg.cs#L36): `public bool Equals(AngleMdeg other)`
- [L41](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/AngleMdeg.cs#L41): `public override bool Equals(object obj)`
- [L46](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/AngleMdeg.cs#L46): `public override int GetHashCode()`
- [L51](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/AngleMdeg.cs#L51): `public override string ToString()`

### PanoramaControlGitRepo/PanoramaControl/Domain/CoordinateContract.cs

- [L9](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/CoordinateContract.cs#L9): `public static AngleMdeg UiToDevice(AngleMdeg value)`
- [L14](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/CoordinateContract.cs#L14): `public static AngleMdeg DeviceToUi(AngleMdeg value)`
- [L20](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/CoordinateContract.cs#L20): `public static AngleMdeg UiTiltToDevice(AngleMdeg value)`
- [L21](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/CoordinateContract.cs#L21): `public static AngleMdeg DeviceTiltToUi(AngleMdeg value)`

### PanoramaControlGitRepo/PanoramaControl/Domain/DeviceCapabilities.cs

- [L5](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/DeviceCapabilities.cs#L5): `public DeviceCapabilities(AngleMdeg panMinimum, AngleMdeg panMaximum, AngleMdeg tiltMinimum, AngleMdeg tiltMaximum, bool safetyLimitsConfirmed)`

### PanoramaControlGitRepo/PanoramaControl/Domain/DeviceStatus.cs

- [L5](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/DeviceStatus.cs#L5): `public DeviceStatus()`

### PanoramaControlGitRepo/PanoramaControl/Domain/PanoramaPlan.cs

- [L15](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/PanoramaPlan.cs#L15): `public PanoramaPlan( uint runId, MoveMode moveMode, AngleMdeg startPan, AngleMdeg startTilt, ushort columns, ushort rows, AngleMdeg stepPan, AngleMdeg stepTilt, uint settleTimeMs, uint triggerPulseMs, uint exposureWaitMs, ushort panSpeedPermille, ushort tiltSpeedPermille, DisconnectPolicy disconnectPolicy, uint heartbeatTimeoutMs, AngleMdeg? sphericalEndPan = null, AngleMdeg? sphericalEndTilt = null)`
- [L86](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/PanoramaPlan.cs#L86): `public static PanoramaPlan FromProtocolFields(IReadOnlyList<string> f)`
- [L97](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/PanoramaPlan.cs#L97): `public int ShotsInBand(int band)`
- [L105](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/PanoramaPlan.cs#L105): `public TargetPosition GetTarget(uint imageIndex)`
- [L135](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/PanoramaPlan.cs#L135): `public IList<string> Validate(DeviceCapabilities capabilities)`
- [L213](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/PanoramaPlan.cs#L213): `public string[] ToProtocolFields()`
- [L245](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/PanoramaPlan.cs#L245): `public TargetPosition(uint imageIndex, ushort column, ushort row, AngleMdeg pan, AngleMdeg tilt)`

### PanoramaControlGitRepo/PanoramaControl/Domain/RunSegmentResolver.cs

- [L11](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/RunSegmentResolver.cs#L11): `public static bool TryResolve(IList<RunSegment> segments, uint zeroBasedCapture, out RunSegment resolvedSegment, out uint imageIndex)`

### PanoramaControlGitRepo/PanoramaControl/Domain/SessionFeatures.cs

- [L20](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L20): `public void Accept(string type, IReadOnlyList<string> fields)`
- [L56](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L56): `private static bool Flag(IReadOnlyList<string> f)`
- [L62](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L62): `private static int Velocity(IReadOnlyList<string> f, int max)`
- [L68](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L68): `internal static void Count(IReadOnlyList<string> f, int count)`
- [L69](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L69): `internal static int Int(string s)`
- [L70](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L70): `internal static uint UInt(string s)`
- [L83](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L83): `public static ManualActionStatus Parse(IReadOnlyList<string> f)`
- [L107](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L107): `internal ManualActionProgress(ushort sequence, string kind, int pan, int tilt, long baseline)`
- [L109](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L109): `internal bool CompletedBy(ManualActionStatus status, long version)`
- [L122](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L122): `internal Sample(int pan, int tilt, bool joined)`
- [L128](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L128): `public Sample[] Snapshot()`
- [L129](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L129): `public void Break()`
- [L130](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L130): `public void Observe(string key, int pan, int tilt, long millis)`
- [L141](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L141): `public static string DescribeInvariant(int flags)`
- [L150](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SessionFeatures.cs#L150): `public static string Describe(int flags)`

### PanoramaControlGitRepo/PanoramaControl/Domain/SphericalGeometry.cs

- [L9](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SphericalGeometry.cs#L9): `public static int TiltAt(int start, int end, int band, int bands)`
- [L14](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SphericalGeometry.cs#L14): `public static int PanAt(int start, int end, int shot, int shots)`
- [L21](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SphericalGeometry.cs#L21): `public static int ShotsInBand(int startPan, int endPan, int tilt, int desiredPanStep)`
- [L37](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Domain/SphericalGeometry.cs#L37): `private static long RoundDivide(long value, long divisor)`

### PanoramaControlGitRepo/PanoramaControl/Form1.cs

- [L49](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L49): `public void ShowCommentOutput(String CommentText)`
- [L56](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L56): `public PanoControlForm()`
- [L58](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L58): `public PanoControlForm(ISerialTransport transport, bool offline, string stateDirectory = null)`
- [L210](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L210): `private void comm_FramePopulatedHandler(object sender, EventArgs e)`
- [L215](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L215): `public void UpdateServoPosition()`
- [L229](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L229): `public void UpdateServoPosWithParams(double panAn, double tiltAn)`
- [L236](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L236): `public double GetShowCoordinate(double value, int axisNum)`
- [L250](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L250): `private async void MoveLeftButton_Click(object sender, EventArgs e)`
- [L255](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L255): `private async void MoveRightButton_Click(object sender, EventArgs e)`
- [L260](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L260): `private async void MoveUpButton_Click(object sender, EventArgs e)`
- [L265](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L265): `private async void MoveDownButton_Click(object sender, EventArgs e)`
- [L270](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L270): `private async void ConnectButton_Click(object sender, EventArgs e)`
- [L275](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L275): `private async void DisconnectButton_Click(object sender, EventArgs e)`
- [L285](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L285): `private async void SetStartPointButton_Click(object sender, EventArgs e)`
- [L290](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L290): `private async Task SetStartPointAsync()`
- [L316](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L316): `private async void SetEndPointButton_Click(object sender, EventArgs e)`
- [L321](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L321): `private async Task SetEndPointAsync()`
- [L349](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L349): `private void UpdateAcquisitionGrid()`
- [L367](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L367): `private void CalculateAcquisitionGrid()`
- [L429](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L429): `public void SetCameraParameters()`
- [L436](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L436): `private async void StartButton_Click(object sender, EventArgs e)`
- [L450](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L450): `private async void StopButton_Click(object sender, EventArgs e)`
- [L474](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L474): `private void Worker_AcquirePanorama(object sender, DoWorkEventArgs e)`
- [L489](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L489): `private void Worker_ProgressChanged(object sender, ProgressChangedEventArgs e)`
- [L506](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L506): `private void Worker_RunWorkerCompleted(object sender, RunWorkerCompletedEventArgs e)`
- [L514](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L514): `private void PanoControl_FormClosing(object sender, FormClosingEventArgs e)`
- [L527](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L527): `private async void ShutterTestButton_Click(object sender, EventArgs e)`
- [L546](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L546): `private void FocalLengthTextBox_Leave(object sender, EventArgs e)`
- [L563](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L563): `private void OverlapTextBox_Leave(object sender, EventArgs e)`
- [L571](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L571): `private void ChipWTextBox_Leave(object sender, EventArgs e)`
- [L586](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L586): `private void ChipHTextBox_Leave(object sender, EventArgs e)`
- [L600](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L600): `private void NumXTextBox_Leave(object sender, EventArgs e)`
- [L609](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L609): `private void NumYTextBox_Leave(object sender, EventArgs e)`
- [L618](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L618): `private async void VibDelayTextBox_Leave(object sender, EventArgs e)`
- [L626](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L626): `private async void ExpDelayTextBox_Leave(object sender, EventArgs e)`
- [L635](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L635): `private async void CamTriggerDurationTextBox_Leave(object sender, EventArgs e)`
- [L642](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L642): `private void TimingTextBox_KeyDown(object sender, KeyEventArgs e)`
- [L649](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L649): `private async Task ApplyTimingIfActiveAsync()`
- [L677](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L677): `private void SpeedXTextBox_Leave(object sender, EventArgs e)`
- [L683](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L683): `private void SpeedYTextBox_Leave(object sender, EventArgs e)`
- [L689](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L689): `private void DrawPanelGrid()`
- [L710](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L710): `private async void panel1_MouseDown(object sender, MouseEventArgs e)`
- [L718](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L718): `private void UpdatePanel()`
- [L726](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L726): `private Point ToPanelPoint(double pan, double tilt)`
- [L732](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L732): `private static void DrawCross(Graphics graphics, Pen pen, Point point)`
- [L737](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L737): `private void DrawEndpoint(Graphics graphics, Pen pen, double pan, double tilt, string caption)`
- [L749](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L749): `private void DrawCapturePoint(double pan, double tilt)`
- [L757](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L757): `private void DrawSphericalPreview(PanoramaPlan plan)`
- [L783](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L783): `private void ClearSphericalPreview()`
- [L791](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L791): `private void RefreshSphericalPreviewAfterSettingsChange()`
- [L797](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L797): `private void panel1_Paint(object sender, PaintEventArgs e)`
- [L813](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L813): `private void ClearPanelButton_Click(object sender, EventArgs e)`
- [L824](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L824): `private async void CamSelectCombo_SelectedIndexChanged(object sender, EventArgs e)`
- [L847](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L847): `private async void PauseButton_Click(object sender, EventArgs e)`
- [L866](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L866): `private async void ContinueButton_Click(object sender, EventArgs e)`
- [L882](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L882): `private async void ColBackButton_Click(object sender, EventArgs e)`
- [L904](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L904): `private void multiShutter(int NumShutterActivations)`
- [L914](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L914): `private void zeroXPosButton_Click(object sender, EventArgs e)`
- [L919](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L919): `private void zeroYPosButton_Click(object sender, EventArgs e)`
- [L924](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L924): `private void UseFlashResponseCheckBox_CheckedChanged(object sender, EventArgs e)`
- [L936](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L936): `private async void ControlTriggerDurationCheckBox_CheckedChanged(object sender, EventArgs e)`
- [L950](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L950): `private void ApplySpeedTestButton_Click(object sender, EventArgs e)`
- [L955](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L955): `private void VibrationCheckBox_CheckedChanged(object sender, EventArgs e)`
- [L968](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L968): `private async void ButtonStartTimelapse_Click(object sender, EventArgs e)`
- [L999](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L999): `private async void ColForwardButton_Click(object sender, EventArgs e)`
- [L1029](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1029): `private async void ButtonStopTimeLapse_Click(object sender, EventArgs e)`
- [L1043](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1043): `private async Task ConnectV2Async(bool useSelectedPort)`
- [L1059](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1059): `private async Task MoveManualStepAsync(double panDelta, double tiltDelta)`
- [L1085](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1085): `private async Task<bool> MoveManualAsync(double panDegrees, double tiltDegrees)`
- [L1097](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1097): `private async Task<bool> SendManualMoveAsync(double panDegrees, double tiltDegrees)`
- [L1114](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1114): `private void InitializeTiltLimitsControls()`
- [L1133](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1133): `private void SessionController_CapabilitiesChanged(object sender, DeviceCapabilitiesEventArgs e)`
- [L1149](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1149): `private void UpdateTiltLimitsButtons()`
- [L1163](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1163): `private async void ReadTiltLimitsButton_Click(object sender, EventArgs e)`
- [L1173](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1173): `private async void SaveTiltLimitsButton_Click(object sender, EventArgs e)`
- [L1196](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1196): `private void SessionController_Message(object sender, SessionMessageEventArgs e)`
- [L1201](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1201): `private void SessionController_ConnectionStateChanged(object sender, ConnectionStateEventArgs e)`
- [L1219](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1219): `private void UpdateConnectionUi(ConnectionState state)`
- [L1234](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1234): `private void SessionController_StatusChanged(object sender, DeviceStatusEventArgs e)`
- [L1280](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1280): `private void UpdateRasterPosition(DeviceStatus status)`
- [L1302](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1302): `private void UpdateTimelapseDisplay(DeviceStatus status)`
- [L1337](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1337): `private void ScanModeComboBox_SelectedIndexChanged(object sender, EventArgs e)`
- [L1350](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1350): `private void ScanModeComboBox_DrawItem(object sender, DrawItemEventArgs e)`
- [L1365](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1365): `private void SessionController_DiagnosticsChanged(object sender, DeviceDiagnosticsEventArgs e)`
- [L1386](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1386): `private void SessionController_RunEventReceived(object sender, RunEventEventArgs e)`
- [L1415](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1415): `private bool ManualMovementAllowed()`
- [L1420](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1420): `private void SetRunButtons(RunState state)`
- [L1446](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1446): `private void ClosePositionLog()`
- [L1451](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1451): `private void EnsurePositionLog()`
- [L1456](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1456): `private void ShowOperationError(string operation, Exception exception)`
- [L1461](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1461): `private void PostToUi(Action action)`
- [L1468](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1468): `private uint CreateRunId()`
- [L1477](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1477): `public int ValidateInt(object _Data, int _DefaultVal, int _MinVal, int _MaxVal)`
- [L1505](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1505): `public double ValidateDouble(object _Data, double _DefaultVal, double _MinVal, double _MaxVal)`
- [L1535](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1535): `private void PanoControlForm_Load(object sender, EventArgs e)`
- [L1543](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.cs#L1543): `public BufferedPanel()`

### PanoramaControlGitRepo/PanoramaControl/Form1.Current.cs

- [L34](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L34): `protected override void WndProc(ref System.Windows.Forms.Message message)`
- [L65](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L65): `private void ReleasePointer(string reason)`
- [L71](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L71): `private static bool HoldKey(Keys key)`
- [L73](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L73): `protected override bool IsInputKey(Keys keyData)`
- [L75](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L75): `protected override void OnKeyDown(KeyEventArgs e)`
- [L82](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L82): `protected override void OnKeyUp(KeyEventArgs e)`
- [L88](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L88): `protected override void OnLostFocus(EventArgs e)`
- [L89](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L89): `private void ReleaseKey()`
- [L136](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L136): `private Button ActionButton(Control parent, string text, Func<Task> action, int width = 110)`
- [L142](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L142): `private async Task UiActionAsync(Func<Task> action)`
- [L149](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L149): `private void InitializeCurrentControls()`
- [L259](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L259): `private async Task EmergencyStopUiAsync()`
- [L267](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L267): `private void ShowTimingDialog(IWin32Window owner)`
- [L271](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L271): `private Form CreateTimingDialog()`
- [L320](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L320): `private void WireArrow(Button b, int axis, int direction)`
- [L357](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L357): `private void ClearArrowClickSuppression()`
- [L359](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L359): `private async Task<int> BeginArrowHoldAsync(Button b, int axis, int direction)`
- [L374](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L374): `private int ArrowVelocity(int axis)`
- [L380](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L380): `private async Task ArrowClickAsync(Button b, int axis, int direction)`
- [L391](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L391): `private async Task ReleaseJogAsync(string reason = "RELEASE")`
- [L400](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L400): `private void RefreshCurrentState()`
- [L478](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L478): `private void UpdateTiltInputRange(DeviceCapabilities capabilities)`
- [L492](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L492): `private PanoramaPlan CreateDraft()`
- [L496](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L496): `private void CommitPlanInputs()`
- [L504](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L504): `private void RebuildDraftFromInputs(bool reportError)`
- [L533](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L533): `private static double ReadNumber(TextBox box, double min, double max)`
- [L540](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L540): `private async Task UploadPlanAsync()`
- [L561](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L561): `private async Task<bool> PrepareRememberAsync()`
- [L577](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L577): `private void SelectPlotTarget(MouseEventArgs e)`
- [L584](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L584): `private async Task DriveSelectedAsync()`
- [L591](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L591): `private async Task DriveEndpointAsync(bool start)`
- [L596](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L596): `private Task ChangeView(double factor)`
- [L602](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L602): `private void SetTiltDisplayRange()`
- [L607](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L607): `private Task FitPlanAsync()`
- [L616](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L616): `private void DrawCurrentPlot(Graphics g)`
- [L668](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L668): `private Task ShowLargePlotAsync()`
- [L684](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L684): `private string CurrentArchiveKey(uint run)`
- [L685](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L685): `private void ArchiveCapture(RunEvent capture)`
- [L700](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L700): `private void InitializeArchiveControls()`
- [L729](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L729): `private void RefreshCaptureList()`
- [L747](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L747): `private Task ExportArchiveAsync()`
- [L761](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L761): `private async Task ExportSupportAsync()`
- [L768](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L768): `private void InitializePersistence()`
- [L797](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L797): `private void SaveSetting(string key, string value)`
- [L799](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L799): `private int SettingInt(string key, int fallback)`
- [L800](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L800): `private void SaveEndpoints()`
- [L808](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L808): `private void MarkDraftForReview()`
- [L814](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L814): `private void ConfirmDraftReference()`
- [L820](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L820): `private void RestoreEndpoints()`
- [L831](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L831): `private void SaveProfile()`
- [L845](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L845): `private void ApplySavedProfile(string name)`
- [L855](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L855): `private void ShowMotionOptions()`
- [L895](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L895): `private void ShowIncident()`
- [L905](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L905): `private async Task AttemptRecoveryAsync(bool requestedByUser)`
- [L920](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L920): `private void ShowCoordinateDialog()`
- [L957](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Current.cs#L957): `private bool FinishClosing(FormClosingEventArgs e)`

### PanoramaControlGitRepo/PanoramaControl/Form1.Parity.cs

- [L20](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Parity.cs#L20): `public void Begin()`
- [L21](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Parity.cs#L21): `public void Write(RunEvent capture, uint id)`
- [L22](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Parity.cs#L22): `public void End()`
- [L24](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Parity.cs#L24): `private void RefreshBackupStatus()`
- [L30](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Parity.cs#L30): `private Task ShowRestDialogAsync()`
- [L69](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Parity.cs#L69): `private void AddParityOptions(FlowLayoutPanel layout, Form dialog)`
- [L118](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Parity.cs#L118): `private void InitializeLanguageControls()`
- [L119](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Parity.cs#L119): `private void ApplyLanguage(Control parent)`
- [L148](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Parity.cs#L148): `private static IEnumerable<Control> Descendants(Control parent)`
- [L150](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Parity.cs#L150): `private void CloseArchive()`
- [L159](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Form1.Parity.cs#L159): `private void ReleaseSessionResources()`

### PanoramaControlGitRepo/PanoramaControl/muControllerInterface.cs

- [L33](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L33): `public static void OpenPort(String ComPort, int BaudRate)`
- [L59](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L59): `public static void ClosePort()`
- [L70](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L70): `static void muControllerDataReceived(object sender, SerialDataReceivedEventArgs e)`
- [L148](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L148): `public static void SetAxisZeroPosition(int axis)`
- [L165](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L165): `public static bool IsAxisAtTarget(int axis)`
- [L211](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L211): `public static void InitServo(double CurServoPosX, double CurServoPosY, double ServoSpeedX, double ServoSpeedY, String COMPortName, int BTbaudrate)`
- [L234](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L234): `public static int MoveServo(string ComPort, int BaudRate, int ServoNumber, double ServoPos)`
- [L285](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L285): `public static void MoveServosToPosition(double x, double y)`
- [L338](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L338): `public static int MovePositionManually(double ax, double ay)`
- [L362](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L362): `public static int SetServoSpeed(string ComPort, int BaudRate, int ServoNumber, double ServoSpeed)`
- [L389](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L389): `public static int SetShutDownThreshold(string ComPort, int BaudRate, int ServoNumber, double ShutDownThreshold)`
- [L421](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L421): `public static void ReqAccelerationSensor()`
- [L427](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L427): `public static void ReqPanSensor()`
- [L433](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L433): `public static void ReqTiltSensor()`
- [L439](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L439): `public static bool ReqCameraStatus()`
- [L447](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L447): `public static void ResetCameraStatus()`
- [L455](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L455): `public static void CamShutterReleaseFixDuration()`
- [L461](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L461): `public static void CamShutterRelease(double CamTriggerDuration)`
- [L470](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/muControllerInterface.cs#L470): `public static List<String> GetAvailableCOMPorts()`

### PanoramaControlGitRepo/PanoramaControl/PanoControlApp.cs

- [L14](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoControlApp.cs#L14): `static void Main()`

### PanoramaControlGitRepo/PanoramaControl/PanoramaAcquisitionClass.cs

- [L10](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs#L10): `public delegate void OnUpdateServoPositionDelegate()`
- [L13](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs#L13): `public void UpdateServoPosition(object sender)`
- [L26](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs#L26): `public delegate void OnUpdateServoPositionWithParamsDelegate(double pan, double tilt)`
- [L28](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs#L28): `public void UpdateServoPosWithParams(double pan, double tilt, object sender)`
- [L41](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs#L41): `public delegate void OnShowCommentOutputDelegate(String text)`
- [L43](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs#L43): `public void ShowNewCommentInGUI(String commentText,object sender)`
- [L71](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs#L71): `public void AcquirePanorama(double start_X, double end_X, double delta_X, double start_Y, double end_Y, double delta_Y, int panoAcquisitionMode, BackgroundWorker bgw, object sender, DoWorkEventArgs e)`
- [L753](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs#L753): `public bool TargetReached(int axis)`
- [L768](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs#L768): `private void GetAndSaveAngleCoords()`
- [L810](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs#L810): `public void CamShutter()`
- [L828](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PanoramaAcquisitionClass.cs#L828): `private void multiShutter(int NumShutterActivations)`

### PanoramaControlGitRepo/PanoramaControl/PapyWizardHelper.cs

- [L19](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PapyWizardHelper.cs#L19): `public static void SaveCoords(double x, double y, double sCNum, int trueCoords)`
- [L54](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PapyWizardHelper.cs#L54): `public static void CreatePositionDataXMLFiles()`
- [L88](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PapyWizardHelper.cs#L88): `public static void SaveCaptureManifest(RunEvent imageEvent, uint pictureId)`
- [L127](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PapyWizardHelper.cs#L127): `public static void ClosePositionDataXMLFiles()`
- [L133](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/PapyWizardHelper.cs#L133): `public static void WriteToFile(String Filename, String Textline)`

### PanoramaControlGitRepo/PanoramaControl/Params.cs

- [L165](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Params.cs#L165): `public static void ApplyCameraSelection(int guiIndex)`
- [L178](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Params.cs#L178): `public static void SetDefaultValues()`
- [L293](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Params.cs#L293): `public static void readConfigFile()`
- [L308](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Params.cs#L308): `public static void readConfigFile(string appConfigFilePath)`

### PanoramaControlGitRepo/PanoramaControl/Protocol/V2/CommandResult.cs

- [L5](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/CommandResult.cs#L5): `public CommandResult(bool accepted, string code)`

### PanoramaControlGitRepo/PanoramaControl/Protocol/V2/Crc16Ccitt.cs

- [L7](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/Crc16Ccitt.cs#L7): `public static ushort Compute(string asciiText)`
- [L12](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/Crc16Ccitt.cs#L12): `public static ushort Compute(byte[] data)`

### PanoramaControlGitRepo/PanoramaControl/Protocol/V2/ProtocolFrame.cs

- [L8](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolFrame.cs#L8): `public ProtocolFrame(ushort sequence, string messageType, params string[] fields)`

### PanoramaControlGitRepo/PanoramaControl/Protocol/V2/ProtocolFrameCodec.cs

- [L12](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolFrameCodec.cs#L12): `public static string Encode(ProtocolFrame frame)`
- [L39](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolFrameCodec.cs#L39): `public static bool TryDecode(string line, out ProtocolFrame frame, out FrameDecodeError error)`
- [L111](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolFrameCodec.cs#L111): `private static void ValidateToken(string value, string parameterName)`
- [L123](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolFrameCodec.cs#L123): `private static bool IsAscii(string value)`

### PanoramaControlGitRepo/PanoramaControl/Protocol/V2/ProtocolV2Client.cs

- [L26](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L26): `public ProtocolV2Client(ISerialTransport transport)`
- [L40](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L40): `public void CancelPending(string reason)`
- [L52](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L52): `public async Task<CommandResult> SendCommandAsync(string messageType, CancellationToken cancellationToken, params string[] fields)`
- [L93](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L93): `private async Task<CommandResult> EmergencyStopAsync(CancellationToken cancellationToken, string[] fields)`
- [L124](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L124): `private async Task SendFrameAsync(PendingCommand request, string encoded, string detail, CancellationToken cancellationToken)`
- [L169](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L169): `public void SendUnconfirmed(string messageType, params string[] fields)`
- [L183](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L183): `private void TransportLineReceived(object sender, SerialLineReceivedEventArgs eventArgs)`
- [L204](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L204): `private void CompletePending(ProtocolFrame frame)`
- [L223](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L223): `private ushort NextSequence()`
- [L234](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L234): `private void RaiseProtocolError(string message, string rawData)`
- [L240](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L240): `private void RaiseTrace(string direction, string rawData, string detail)`
- [L246](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L246): `private void ThrowIfDisposed()`
- [L251](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L251): `public void Dispose()`
- [L261](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L261): `public PendingCommand(ushort sequence, TaskCompletionSource<CommandResult> completion)`
- [L274](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L274): `public ProtocolTraceEventArgs(string direction, string rawData, string detail)`
- [L287](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L287): `public ProtocolFrameEventArgs(ProtocolFrame frame)`
- [L297](https://github.com/TomNaiser/PanoramaControl/blob/master/PanoramaControl/Protocol/V2/ProtocolV2Client.cs#L297): `public ProtocolErrorEventArgs(string message, string rawData)`
