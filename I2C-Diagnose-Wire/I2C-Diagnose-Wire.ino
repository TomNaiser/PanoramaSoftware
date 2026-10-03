// Voruebergehender I2C-Test fuer PanoBoard V5 / Nano R4.
// Keine Ansteuerung von Motor oder Kamera, keine Sensorregister werden veraendert.
// Beide AS5048B-Encoder fuer diesen Test anschliessen, Motorversorgung auslassen.

#include <Wire.h>

void setup() {
  Serial.begin(115200);
  delay(1500);
  Wire.begin();  // Genau einmal, anders als in der bisherigen Sensorbibliothek.
  Serial.println("I2C-Test: Wire.begin() einmal; Registerpointer 0xFE");
}

void loop() {
  const uint8_t addresses[] = {0x40, 0x41};
  for (uint8_t address : addresses) {
    Wire.beginTransmission(address);
    Wire.write(0xFE);  // Nur Adresse des Winkelregisters setzen.
    uint8_t result = Wire.endTransmission(true);  // STOP, kein Repeated Start.
    Serial.print("0x");
    Serial.print(address, HEX);
    Serial.print(" -> ");
    Serial.println(result);
  }
  delay(3000);
}
