// Voruebergehender Hardwaretest fuer PanoBoard V5 / Nano R4.
// Keine I2C-Transaktionen und keine Ansteuerung von Motor oder Kamera.
// Encoder und Motorversorgung fuer den ersten Test abziehen.

void setup() {
  pinMode(A4, INPUT_PULLUP);
  pinMode(A5, INPUT_PULLUP);
  Serial.begin(115200);
  delay(1500);
  Serial.println("I2C-Diagnose: A4/A5 als INPUT_PULLUP, kein Wire.begin()");
}

void loop() {
  Serial.print("A4/SDA=");
  Serial.print(digitalRead(A4) == HIGH ? "HIGH" : "LOW");
  Serial.print("  A5/SCL=");
  Serial.println(digitalRead(A5) == HIGH ? "HIGH" : "LOW");
  delay(1000);
}
